import os
from typing import List, Any, Union, Dict, TypedDict, Annotated, Sequence
from langchain.prompts import StringPromptTemplate
from langchain_ollama import OllamaLLM
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage
import requests
from dotenv import load_dotenv
import logging
from typing import ClassVar
from langgraph.graph import StateGraph, END
from langgraph.prebuilt import ToolNode
from langchain_core.tools import BaseTool

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')
logger = logging.getLogger(__name__)

# Load environment variables
load_dotenv()

# Initialize API keys
ALPHA_VANTAGE_API_KEY = os.getenv("ALPHA_VANTAGE_API_KEY")

# Model configuration
MODEL = 'llama3.2'  # using the same model as in app.py

class StockDataTool(BaseTool):
    name: str = "StockData"
    description: str = "Useful for getting stock market data. Input should be a stock symbol (e.g., AAPL, GOOGL)."

    def _run(self, symbol: str) -> str:
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

    def _arun(self, symbol: str) -> str:
        """Async version - just calls sync version for now"""
        return self._run(symbol)

# Create our state type
class AgentState(TypedDict):
    messages: Annotated[Sequence[BaseMessage], "The messages in the conversation"]
    next: str

# Define the prompt template
AGENT_PROMPT = """You are a stock market expert assistant. Your task is to help users get stock market data using the available tools.

Available tools:
{tools}

IMPORTANT INSTRUCTIONS:
1. To get stock data, you MUST use the exact format: StockData(SYMBOL)
   Example: StockData(AAPL)

2. After getting the data, you MUST provide a clear summary of the stock information.

Remember:
- Always use the tool to get data before providing information
- Be precise and professional in your responses

Current conversation:
{messages}

Question: {input}
Assistant: Let me help you with that stock information."""

def create_stock_agent():
    # Initialize tools
    tools = [StockDataTool()]
    
    # Initialize Ollama LLM
    llm = OllamaLLM(
        model=MODEL,
        temperature=0
    )
    
    # Create tool node
    tool_node = ToolNode(tools=tools)
    
    # Function to determine if we should continue processing
    def should_continue(state: AgentState) -> bool:
        """Return True if we should continue processing."""
        last_message = state["messages"][-1].content
        return "StockData" in last_message
    
    # Function to call the model
    def call_model(state: AgentState) -> AgentState:
        """Call the model to get the next action."""
        messages = state["messages"]
        tools_str = "\n".join([f"{tool.name}: {tool.description}" for tool in tools])
        
        # Format prompt with tools and messages
        prompt = AGENT_PROMPT.format(
            tools=tools_str,
            messages="\n".join(str(m.content) for m in messages),
            input=messages[-1].content if messages else ""
        )
        
        # Get model response
        response = llm.invoke(prompt)
        
        # Add AI message to state
        state["messages"].append(AIMessage(content=response))
        
        # Determine next step
        state["next"] = "call_tool" if should_continue(state) else END
        
        return state
    
    # Function to call tool
    def call_tool(state: AgentState) -> AgentState:
        """Call the appropriate tool."""
        last_message = state["messages"][-1].content
        
        # Extract tool call from message
        if "StockData" in last_message:
            # Extract symbol from StockData(SYMBOL)
            start = last_message.find("StockData(") + len("StockData(")
            end = last_message.find(")", start)
            symbol = last_message[start:end].strip()
            
            # Create tool message
            tool_message = {
                "messages": [{"content": f"StockData({symbol})", "type": "tool"}],
                "name": "StockData",
                "input": symbol
            }
            
            # Call tool
            result = tool_node.invoke(tool_message)
            
            # Add result to messages
            state["messages"].append(AIMessage(content=f"Tool response: {result}"))
            
        # Always go back to the model after tool use
        state["next"] = "call_model"
        return state
    
    # Create the graph
    workflow = StateGraph(AgentState)
    
    # Add nodes
    workflow.add_node("call_model", call_model)
    workflow.add_node("call_tool", call_tool)
    
    # Add edges
    workflow.add_edge("call_model", "call_tool")
    workflow.add_edge("call_tool", "call_model")
    
    # Set entry point
    workflow.set_entry_point("call_model")
    
    # Compile the graph
    app = workflow.compile()
    
    return app

if __name__ == "__main__":
    # Example usage
    agent = create_stock_agent()
    
    # Test the stock data tool directly first
    tools = [StockDataTool()]
    stock_tool = tools[0]
    print("\nTesting StockData tool directly:")
    result = stock_tool._run("AAPL")
    print(result)
    
    print("\nTesting agent with the same query:")
    result = agent.invoke({
        "messages": [HumanMessage(content="What is the current price of AAPL stock?")],
        "next": "call_model"
    })
    
    # Print the conversation
    print("\nConversation:")
    for message in result["messages"]:
        role = "Human" if isinstance(message, HumanMessage) else "Assistant"
        print(f"{role}: {message.content}\n")
