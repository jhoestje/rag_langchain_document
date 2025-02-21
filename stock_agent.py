import os
from typing import List, Any, Union, Dict, TypedDict, Annotated, Sequence
from langchain.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_ollama import OllamaLLM
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage, FunctionMessage
import requests
from dotenv import load_dotenv
import logging
from typing import ClassVar
from langgraph.graph import StateGraph, END
from langgraph.prebuilt import ToolInvocation
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

# Define our state
class AgentState(TypedDict):
    messages: Annotated[Sequence[BaseMessage], "The messages in the conversation"]
    next: str

def create_stock_agent():
    # Initialize tools
    tools = [StockDataTool()]
    
    # Initialize Ollama LLM
    llm = OllamaLLM(
        model=MODEL,
        temperature=0
    )
    
    # Create the prompt template
    prompt = ChatPromptTemplate.from_messages([
        ("system", """You are a stock market expert assistant. Your task is to help users get stock market data.

When you need stock data, use the StockData tool with the stock symbol.
After getting the data, provide a clear summary and end the conversation.
Do not ask follow-up questions."""),
        MessagesPlaceholder(variable_name="messages"),
        ("human", "{input}")
    ])
    
    # Function to determine if we should continue processing
    def should_continue(state: AgentState) -> bool:
        """Return True if we should continue processing."""
        if not state["messages"]:
            return False
        
        last_message = state["messages"][-1]
        # Stop if we've already used the tool and got a response
        if any(isinstance(msg, FunctionMessage) for msg in state["messages"]):
            return False
        return True
    
    # Function to call the model
    def call_model(state: AgentState) -> AgentState:
        """Call the model to get the next action."""
        messages = state["messages"]
        
        # Get the last human message for input
        last_human_msg = next((msg for msg in reversed(messages) 
                             if isinstance(msg, HumanMessage)), None)
        
        if not last_human_msg:
            state["next"] = END
            return state
            
        # Format prompt
        model_response = llm.invoke(
            prompt.format_messages(
                messages=messages,
                input=last_human_msg.content
            )
        )
        
        # Add AI message to state
        state["messages"].append(AIMessage(content=model_response))
        
        # Check if we need to call a tool
        if "StockData" in model_response:
            state["next"] = "call_tool"
        else:
            state["next"] = END
            
        return state
    
    # Function to call tool
    def call_tool(state: AgentState) -> AgentState:
        """Call the appropriate tool."""
        last_message = state["messages"][-1].content
        
        # Extract symbol from StockData(SYMBOL)
        if "StockData" in last_message:
            start = last_message.find("StockData(") + len("StockData(")
            end = last_message.find(")", start)
            symbol = last_message[start:end].strip()
            
            # Call tool
            tool = tools[0]
            result = tool._run(symbol)
            
            # Add result to messages
            state["messages"].append(FunctionMessage(
                content=result,
                name="StockData"
            ))
            
            # Go back to model for final response
            state["next"] = "call_model"
        else:
            state["next"] = END
            
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
        if isinstance(message, HumanMessage):
            print(f"Human: {message.content}")
        elif isinstance(message, AIMessage):
            print(f"Assistant: {message.content}")
        elif isinstance(message, FunctionMessage):
            print(f"Function ({message.name}): {message.content}")
        print()
