import os
from typing import List, Any, Union, Dict, TypedDict, Annotated, Sequence, ClassVar
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_ollama import OllamaLLM, ChatOllama
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage, FunctionMessage
from langchain_core.runnables import RunnablePassthrough
from langgraph.pregel import END
from langgraph.prebuilt import ToolNode
from langchain_core.tools import BaseTool
import requests
from dotenv import load_dotenv
import logging

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')
logger = logging.getLogger(__name__)

# Load environment variables
logger.info("Loading environment variables...")
load_dotenv()

# Initialize API keys
logger.info("Checking for API key...")
ALPHA_VANTAGE_API_KEY = os.getenv("ALPHA_VANTAGE_API_KEY")
logger.info(f"API key present: {bool(ALPHA_VANTAGE_API_KEY)}")

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
    # Create the tools
    tools = [StockDataTool()]
    
    # Create the prompt template
    prompt = ChatPromptTemplate.from_messages([
        ("system", """You are a stock market expert assistant with access to real-time stock data through the StockData tool.

IMPORTANT: To get stock data, use the StockData tool with a stock symbol (e.g., AAPL, GOOGL).
Format your response like this:
Current Price: $X.XX
Summary: A brief summary of the stock's current status."""),
        MessagesPlaceholder(variable_name="chat_history"),
        ("human", "{input}")
    ])

    # Create the model
    model = ChatOllama(model=MODEL)

    # Create the agent
    agent = (
        RunnablePassthrough.assign(
            chat_history=lambda x: x.get("chat_history", []),
            agent_outcome=lambda x: prompt | model
        )
        | ToolNode(tools)
    )

    return agent

if __name__ == "__main__":
    print("\nTesting StockData tool directly:")
    stock_tool = StockDataTool()
    result = stock_tool._run("AAPL")
    print(result)

    print("\nTesting agent with the same query:")
    agent = create_stock_agent()
    result = agent.invoke({
        "input": "What is the current price of AAPL stock?",
        "chat_history": []
    })
    print(result)
