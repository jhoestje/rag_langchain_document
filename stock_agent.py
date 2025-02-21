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
from langchain_core.tools import BaseTool

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')
logger = logging.getLogger(__name__)

# Load environment variables
load_dotenv()

# Initialize API keys
ALPHA_VANTAGE_API_KEY = os.getenv("ALPHA_VANTAGE_API_KEY")
if not ALPHA_VANTAGE_API_KEY:
    raise ValueError("Please set the ALPHA_VANTAGE_API_KEY environment variable in your .env file")

# Model configuration
MODEL = 'llama3.2'  # using the same model as in app.py

class StockDataTool(BaseTool):
    name: str = "StockData"
    description: str = "Useful for getting stock market data. Input should be a stock symbol (e.g., AAPL, GOOGL)."

    def _run(self, symbol: str) -> str:
        """Get stock data from Alpha Vantage API"""
        logger.info(f"Making API request for symbol: {symbol}")
        url = f"https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol={symbol}&apikey={ALPHA_VANTAGE_API_KEY}"
        logger.info(f"Using API key: {ALPHA_VANTAGE_API_KEY[:4]}...")  # Log first 4 chars of API key for debugging
        
        try:
            response = requests.get(url)
            logger.info(f"API Response status code: {response.status_code}")
            logger.info(f"API Response content: {response.text[:200]}...")  # Log first 200 chars of response
            
            data = response.json()
            
            if "Error Message" in data:
                logger.error(f"API Error: {data['Error Message']}")
                return f"Error: {data['Error Message']}"
                
            if "Global Quote" not in data:
                logger.error(f"Unexpected API response format: {data}")
                return f"Error: Could not fetch data for symbol {symbol}. Response: {str(data)}"
                
            quote = data["Global Quote"]
            result = f"""
            Stock: {symbol}
            Price: ${quote.get('05. price', 'N/A')}
            Change: {quote.get('09. change', 'N/A')} ({quote.get('10. change percent', 'N/A')})
            Volume: {quote.get('06. volume', 'N/A')}
            """
            logger.info(f"Successfully fetched data for {symbol}")
            return result
            
        except Exception as e:
            logger.error(f"Exception during API call: {str(e)}")
            return f"Error: Exception while fetching data for symbol {symbol}: {str(e)}"

# Define our state
class AgentState(TypedDict):
    messages: Annotated[Sequence[BaseMessage], "The messages in the conversation"]
    next: str
    tool_used: bool

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
        ("system", """You are a stock market expert assistant with access to real-time stock data through the StockData tool.

IMPORTANT: To get stock data, you MUST use this exact format: StockData(SYMBOL)
For example:
- To get Apple stock data, type: StockData(AAPL)
- To get Google stock data, type: StockData(GOOGL)

DO NOT give general advice or alternatives. ALWAYS use the StockData tool to get current prices.
After getting the data, provide a clear summary and end the conversation.
Do not ask follow-up questions."""),
        MessagesPlaceholder(variable_name="messages"),
        ("human", "{input}")
    ])
    
    # Function to determine if we should continue processing
    def should_continue(state: AgentState) -> bool:
        """Return True if we should continue processing."""
        logger.info("Checking if we should continue processing...")
        
        if not state["messages"]:
            logger.info("No messages in state, stopping")
            return False
        
        last_message = state["messages"][-1]
        logger.info(f"Last message type: {type(last_message)}")
        logger.info(f"Last message content: {last_message.content}")
        
        # Stop if we've already used the tool and got a response
        has_function_message = any(isinstance(msg, FunctionMessage) for msg in state["messages"])
        logger.info(f"Has function message: {has_function_message}")
        
        if has_function_message:
            logger.info("Tool has been used, stopping")
            return False
            
        logger.info("Continuing processing")
        return True
    
    # Function to call the model
    def call_model(state: AgentState) -> AgentState:
        """Call the model to get the next action."""
        logger.info("\n=== Entering call_model ===")
        messages = state["messages"]
        
        # Check if we've already used the tool and got a response
        has_function_message = any(isinstance(msg, FunctionMessage) for msg in state["messages"])
        if has_function_message:
            logger.info("Tool has been used, getting final response")
            # Format prompt for final response
            last_human_msg = next((msg for msg in reversed(messages) 
                                if isinstance(msg, HumanMessage)), None)
            
            if not last_human_msg:
                logger.info("No human message found, ending")
                state["next"] = END
                return state
                
            # Get final response from model
            formatted_messages = prompt.format_messages(
                messages=messages,
                input=last_human_msg.content
            )
            model_response = llm.invoke(formatted_messages)
            state["messages"].append(AIMessage(content=model_response))
            
            logger.info("Final response added, ending conversation")
            state["next"] = END
            return state
        
        # Get the last human message for input
        last_human_msg = next((msg for msg in reversed(messages) 
                             if isinstance(msg, HumanMessage)), None)
        
        if not last_human_msg:
            logger.info("No human message found, ending")
            state["next"] = END
            return state
            
        logger.info(f"Last human message: {last_human_msg.content}")
        
        # Format prompt
        formatted_messages = prompt.format_messages(
            messages=messages,
            input=last_human_msg.content
        )
        logger.info(f"Formatted prompt: {formatted_messages}")
        
        # Get model response
        model_response = llm.invoke(formatted_messages)
        logger.info(f"Model response: {model_response}")
        
        # Add AI message to state
        state["messages"].append(AIMessage(content=model_response))
        
        # Check if we need to call a tool
        if "StockData(" in model_response:
            logger.info("Tool call detected in response, transitioning to call_tool")
            state["next"] = "call_tool"
        else:
            logger.info("No tool call detected, ending conversation")
            state["next"] = END
            
        logger.info(f"Next state: {state['next']}")
        logger.info("=== Exiting call_model ===\n")
        return state
    
    # Function to call tool
    def call_tool(state: AgentState) -> AgentState:
        """Call the appropriate tool."""
        logger.info("\n=== Entering call_tool ===")
        last_message = state["messages"][-1].content
        logger.info(f"Processing message: {last_message}")
        
        # Extract tool call from message
        if "StockData(" in last_message:
            logger.info("Found StockData call")
            # Extract symbol from StockData(SYMBOL)
            start = last_message.find("StockData(") + len("StockData(")
            end = last_message.find(")", start)
            symbol = last_message[start:end].strip()
            logger.info(f"Extracted symbol: {symbol}")
            
            # Call tool
            logger.info("Calling StockData tool")
            result = tools[0]._run(symbol)
            logger.info(f"Tool result: {result}")
            
            # Add result to messages
            state["messages"].append(FunctionMessage(
                content=result,
                name="StockData"
            ))
            logger.info("Added function message to state")
            
            # Go back to model for final response
            state["next"] = "call_model"
            logger.info("Transitioning back to call_model")
        else:
            logger.info("No StockData call found, ending")
            state["next"] = END
            
        logger.info(f"Next state: {state['next']}")
        logger.info("=== Exiting call_tool ===\n")
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
        "next": "call_model",
        "tool_used": False
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
