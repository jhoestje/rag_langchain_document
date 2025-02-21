import os
from typing import List, Any, Union
from langchain.agents import Tool, AgentExecutor, LLMSingleActionAgent
from langchain.prompts import StringPromptTemplate
from langchain_ollama import OllamaLLM
from langchain.chains.llm import LLMChain
from langchain.schema import AgentAction, AgentFinish
from langchain_core.callbacks import BaseCallbackHandler
from langchain.agents.agent import AgentOutputParser
import requests
from dotenv import load_dotenv
import logging
from typing import ClassVar

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')
logger = logging.getLogger(__name__)

# Load environment variables
load_dotenv()

# Initialize API keys
ALPHA_VANTAGE_API_KEY = os.getenv("ALPHA_VANTAGE_API_KEY")

# Model configuration
MODEL = 'llama3.2'  # using the same model as in app.py

class StockDataTool(Tool):
    def __init__(self):
        super().__init__(
            name="StockData",
            func=self._get_stock_data,
            description="Useful for getting stock market data. Input should be a stock symbol (e.g., AAPL, GOOGL)."
        )

    def _get_stock_data(self, symbol: str) -> str:
        """Get stock data from Alpha Vantage API"""
        url = f"https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords={symbol}&apikey={ALPHA_VANTAGE_API_KEY}"
        response = requests.get(url)
        data = response.json()
        
        if "Global Quote" not in data:
            return f"Error: Could not fetch data for symbol {symbol}"
            
        quote = data["Global Quote"]
        return f"""
        Stock: {symbol}
        Price: ${quote.get('05. price', 'N/A')}
        Change: {quote.get('09. change', 'N/A')} ({quote.get('10. change percent', 'N/A')})
        Volume: {quote.get('06. volume', 'N/A')}
        """

class StockPromptTemplate(StringPromptTemplate):
    template: ClassVar[str] = """You are a stock market expert assistant. Your task is to help users get stock market data using the available tools.

Available tools:
{tools}

IMPORTANT INSTRUCTIONS:
1. To get stock data, you MUST use the exact format: StockData(SYMBOL)
   Example: StockData(AAPL)

2. After getting the data, you MUST respond with:
   Final Answer: [Your detailed response based on the data]

3. If you cannot help, respond with:
   Final Answer: I cannot help with that request.

Remember:
- Always use the tool first to get data
- Always format your final response with "Final Answer:"
- Be precise and follow the format exactly

Question: {input}
Thought: Let me help you with that stock information.
{agent_scratchpad}"""

    def format(self, **kwargs) -> str:
        tools_str = "\n".join([f"{tool.name}: {tool.description}" for tool in kwargs["tools"]])
        kwargs["tools"] = tools_str
        return self.template.format(**kwargs)

# Create a custom callback handler to log prompts
class PromptLoggingHandler(BaseCallbackHandler):
    def on_llm_start(self, serialized, prompts, **kwargs):
        for i, prompt in enumerate(prompts):
            logger.info(f"Prompt {i + 1}:\n{prompt}")

# Create a custom callback handler to log Ollama requests
class OllamaRequestLoggingHandler(BaseCallbackHandler):
    def on_llm_start(self, serialized, prompts, **kwargs):
        logger.info(f"\nOllama Request Payload:")
        for i, prompt in enumerate(prompts):
            logger.info(f"Prompt {i + 1}:\n{prompt}\n{'='*50}")

    def on_llm_end(self, response, **kwargs):
        logger.info(f"Ollama Response:\n{response}\n{'='*50}")

    def on_llm_error(self, error, **kwargs):
        logger.error(f"Ollama Error:\n{error}\n{'='*50}")

    def on_chain_start(self, serialized, inputs, **kwargs):
        logger.info(f"\nChain Input:\n{inputs}\n{'='*50}")

    def on_chain_end(self, outputs, **kwargs):
        logger.info(f"\nChain Output:\n{outputs}\n{'='*50}")

    def on_tool_start(self, serialized, input_str: str, **kwargs):
        logger.info(f"\nTool Input:\n{input_str}\n{'='*50}")

    def on_tool_end(self, output: str, **kwargs):
        logger.info(f"Tool Output:\n{output}\n{'='*50}")

    def on_tool_error(self, error: str, **kwargs):
        logger.error(f"Tool Error:\n{error}\n{'='*50}")

class StockAgentOutputParser(AgentOutputParser):
    def parse(self, text: str) -> Union[AgentAction, AgentFinish]:
        if "Final Answer:" in text:
            return AgentFinish(
                return_values={"output": text.split("Final Answer:")[-1].strip()},
                log=text,
            )
        
        # Parse out the action and input
        action_match = text.strip().split("\n")[0]
        if "StockData" not in action_match:
            return AgentFinish(
                return_values={"output": "I cannot help with that request."},
                log=text,
            )
            
        action = "StockData"
        action_input = action_match.split("StockData")[-1].strip()
        
        return AgentAction(tool=action, tool_input=action_input.strip("()"), log=text)

    @property
    def _type(self) -> str:
        return "stock_agent"

def create_stock_agent():
    # Initialize tools
    tools = [StockDataTool()]
    
    # Initialize callback handlers
    callbacks = [
        PromptLoggingHandler(),
        OllamaRequestLoggingHandler()
    ]
    
    # Initialize Ollama LLM
    llm = OllamaLLM(
        model=MODEL,
        temperature=0,
        callbacks=callbacks
    )
    
    # Initialize prompt
    prompt = StockPromptTemplate(
        input_variables=["input", "tools", "agent_scratchpad"]
    )
    
    # Initialize LLM chain
    llm_chain = LLMChain(llm=llm, prompt=prompt)

    # Initialize the agent
    agent = LLMSingleActionAgent(
        llm_chain=llm_chain,
        output_parser=StockAgentOutputParser(),
        stop=["\nObservation:"],
        allowed_tools=[tool.name for tool in tools]
    )
    
    # Create the agent executor with callbacks
    agent_executor = AgentExecutor.from_agent_and_tools(
        agent=agent,
        tools=tools,
        callbacks=callbacks,
        verbose=True
    )
    
    return agent_executor

if __name__ == "__main__":
    # Example usage
    agent = create_stock_agent()
    
    # Test the stock data tool directly first
    tools = [StockDataTool()]
    stock_tool = tools[0]
    print("\nTesting StockData tool directly:")
    result = stock_tool._get_stock_data("AAPL")
    print(result)
    
    print("\nTesting agent with the same query:")
    result = agent.run(
        {
            "input": "What is the current price of AAPL stock?",
            "tools": tools,
            "agent_scratchpad": ""
        }
    )
    print(result)
