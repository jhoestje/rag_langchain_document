from pymongo import MongoClient
from langchain_community.document_loaders import DirectoryLoader, TextLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_ollama import OllamaEmbeddings, OllamaLLM
from langchain_mongodb import MongoDBAtlasVectorSearch
from langchain.chains import ConversationalRetrievalChain
from langchain.memory import ConversationBufferMemory
import logging
from langchain_core.callbacks import BaseCallbackHandler
from langchain_core.retrievers import BaseRetriever
from typing import Any
from pydantic import Field

MODEL = 'llama3.2'

# For local MongoDB
#client = MongoClient('mongodb://localhost:27017/')

# For MongoDB Atlas (replace placeholders with your credentials)
client = MongoClient("mongodb+srv://johnhoestje:V6rmmjEqfB9Sq4hk@cluster0.cqhgn.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0")
#mongodb+srv://johnhoestje:V6rmmjEqfB9Sq4hk@cluster0.cqhgn.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0
db = client['langchain_db']
collection = db['embeddings']

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')
logger = logging.getLogger(__name__)

# Create a custom callback handler to log prompts
class PromptLoggingHandler(BaseCallbackHandler):
    def on_llm_start(self, serialized, prompts, **kwargs):
        for i, prompt in enumerate(prompts):
            logger.info(f"Prompt {i + 1}:\n{prompt}")

# Create vector search index if it doesn't exist
# try:
#     # Check if index exists
#     existing_indexes = collection.list_indexes()
#     index_exists = any(index.get('name') == 'vector_index' for index in existing_indexes)
    
#     if not index_exists:
#         # Create the vector search index
#         search_index_model = SearchIndexModel(
#   definition = {
#     "fields": [
#       {
#         "type": "vector",
#         "numDimensions": 1536,
#         "path": "embedding",
#         "similarity": "cosine"
#       }
#     ]
#   }
# )

#         collection.create_search_index(search_index_model, name="vector_index")
#         print("Vector search index created successfully")
# except Exception as e:
#     print(f"Error creating index: {e}")

# Replace 'path/to/your/documents' with the actual path
loader = DirectoryLoader(r'C:\workspace\rag\langchain\poc\documents', glob='**/*.txt', loader_cls=TextLoader)
documents = loader.load()

text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=50)
docs = text_splitter.split_documents(documents)

embeddings = OllamaEmbeddings(model='llama3.2')

for doc in docs:
    logger.info(f"Embedding page content:\n{doc.page_content}")

embedding_vectors = embeddings.embed_documents([doc.page_content for doc in docs])

#don't keep embedding the same text
#store in MongoDB
for doc, vector in zip(docs, embedding_vectors):
    #doc.embedding = vector
        collection.insert_one({
        'content': doc.page_content,
        'embedding': vector
    })

# Initialize vector store with the correct index name
vectorstore = MongoDBAtlasVectorSearch(
    collection=collection,
    embedding=embeddings,
    index_name="vector_index"
)

# Create a logging wrapper for the retriever
class LoggingRetriever(BaseRetriever):
    retriever: BaseRetriever = Field(description="The base retriever to wrap with logging")
    
    @classmethod
    def from_retriever(cls, retriever: BaseRetriever) -> "LoggingRetriever":
        return cls(retriever=retriever)

    def _get_relevant_documents(self, query: str, *, run_manager: Any = None):
        docs = self.retriever.get_relevant_documents(query)
        logger.info(f"\nSearch results for query: {query}")
        for i, doc in enumerate(docs, 1):
            logger.info(f"\nDocument {i}:\n{doc.page_content}\n{'='*50}")
        return docs

    async def _aget_relevant_documents(self, query: str, *, run_manager: Any = None):
        return await self.retriever.aget_relevant_documents(query)

#The retriever will use your vector store to find relevant chunks of text based on the user's query.
base_retriever = vectorstore.as_retriever()
retriever = LoggingRetriever.from_retriever(base_retriever)

# Initialize the LLM with callbacks
llm = OllamaLLM(
    model=MODEL, 
    temperature=0.5,
    callbacks=[PromptLoggingHandler()]
)

# Initialize the memory
memory = ConversationBufferMemory(memory_key="chat_history", return_messages=True)

# Initialize the chain
conversation = ConversationalRetrievalChain.from_llm(llm=llm, retriever=retriever, memory=memory, verbose=True)

while True:
    try:
        query = input("Enter a query: ")
        if query.lower() == "exit":
            break
        result = conversation.invoke({"question": query})
        print(f"Response: {result['answer']}")
    except Exception as e:
        print(f"Error: {e}")
        print("Please try again or type 'exit' to quit")