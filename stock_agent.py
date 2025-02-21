import os
from typing import List, Any
from langchain.agents import Tool, AgentExecutor, LLMSingleActionAgent
from langchain.prompts import StringPromptTemplate
from langchain_ollama import OllamaLLM
from langchain import LLMChain
from langchain.schema import AgentAction, AgentFinish
from langchain_core.callbacks import BaseCallbackHandler
import requests
from dotenv import load_dotenv
import logging

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')
logger = logging.getLogger(__name__)

# Load environment variables
load_dotenv()

# Initialize API keys
ALPHA_VANTAGE_API_KEY = os.getenv("ALPHA_VANTAGE_API_KEY")

# Model configuration
MODEL = 'llama2'  # or 'llama3.2' if you have it available

class StockDataTool(Tool):
    def __init__(self):
        super().__init__(
            name="StockData",
            func=self._get_stock_data,
            description="Useful for getting stock market data. Input should be a stock symbol (e.g., AAPL, GOOGL)."
        )

    def _get_stock_data(self, symbol: str) -> str:
        """Get stock data from Alpha Vantage API"""
        url = f"https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol={symbol}&apikey={ALPHA_VANTAGE_API_KEY}"
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
    template = """You are a stock market expert assistant. Use the following tools to help answer questions about stocks:

{tools}

Question: {input}
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
        template=StockPromptTemplate.template,
        input_variables=["input", "tools", "agent_scratchpad"]
    )
    
    # Initialize LLM chain
    llm_chain = LLMChain(llm=llm, prompt=prompt)
    
    # Define output parser
    def output_parser(llm_output: str) -> AgentAction or AgentFinish:
        if "Final Answer:" in llm_output:
            return AgentFinish(
                return_values={"output": llm_output.split("Final Answer:")[-1].strip()},
                log=llm_output,
            )
        
        # Parse out the action and input
        action_match = llm_output.strip().split("\n")[0]
        if "StockData" not in action_match:
            return AgentFinish(
                return_values={"output": "I cannot help with that request."},
                log=llm_output,
            )
            
        action = "StockData"
        action_input = action_match.split("StockData")[-1].strip()
        
        return AgentAction(tool=action, tool_input=action_input.strip("()"), log=llm_output)
    
    # Initialize the agent
    agent = LLMSingleActionAgent(
        llm_chain=llm_chain,
        output_parser=output_parser,
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
    result = agent.run("What is the current price of AAPL stock?")
    print(result)
