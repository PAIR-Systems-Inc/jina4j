# Jina4J

A Java client library for [Jina AI's](https://jina.ai/) Embedding and Reranking API.

## Features

- Text and image embeddings using state-of-the-art models
- Document reranking for search result optimization
- Multi-vector embeddings (ColBERT) for fine-grained matching
- OpenAPI-generated client for type safety and reliability

## Installation

### Maven

```xml
<dependency>
    <groupId>com.github.PAIR-Systems-Inc</groupId>
    <artifactId>jina4j</artifactId>
    <version>0.0.1</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.github.PAIR-Systems-Inc:jina4j:0.0.1'
```

### JitPack

Add the JitPack repository:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

Then add the dependency:

```gradle
implementation 'com.github.PAIR-Systems-Inc:jina4j:0.0.1'
```

## Examples

Executable examples are provided in the [examples](examples/) directory along with commands to run them.

### Creating Text Embeddings

```java
import ai.pairsys.jina4j.client.ApiClient;
import ai.pairsys.jina4j.client.Configuration;
import ai.pairsys.jina4j.client.auth.HttpBearerAuth;
import ai.pairsys.jina4j.client.api.EmbeddingsApi;
import ai.pairsys.jina4j.client.model.*;
import java.util.Arrays;
import java.util.List;

// Configure API client
ApiClient defaultClient = Configuration.getDefaultApiClient();

// Set your API key
HttpBearerAuth HTTPBearer = (HttpBearerAuth) defaultClient.getAuthentication("HTTPBearer");
HTTPBearer.setBearerToken("JINA_API_KEY");

// Create embeddings
EmbeddingsApi api = new EmbeddingsApi(defaultClient);
TextEmbeddingInput textInput = new TextEmbeddingInput();
textInput.setModel("jina-embeddings-v3");

// Create input with list of strings
List<String> texts = Arrays.asList(
    "Hello, world!", 
    "Goodmem is awesome!"
);
textInput.setInput(new Input(texts));
textInput.setTask(TextEmbeddingInput.TaskEnum.RETRIEVAL_PASSAGE);
textInput.setDimensions(1024);

EmbeddingInput input = new EmbeddingInput(textInput);
ModelEmbeddingOutput result = api.createEmbeddingV1EmbeddingsPost(input);
```

### Reranking Documents

```java
import ai.pairsys.jina4j.client.ApiClient;
import ai.pairsys.jina4j.client.Configuration;
import ai.pairsys.jina4j.client.auth.HttpBearerAuth;
import ai.pairsys.jina4j.client.api.RerankApi;
import ai.pairsys.jina4j.client.model.*;
import java.util.ArrayList;
import java.util.List;

// Configure API client
ApiClient defaultClient = Configuration.getDefaultApiClient();

// Set your API key
HttpBearerAuth HTTPBearer = (HttpBearerAuth) defaultClient.getAuthentication("HTTPBearer");
HTTPBearer.setBearerToken("JINA_API_KEY");

// Create rerank API instance
RerankApi rerankApi = new RerankApi(defaultClient);
RankAPIInput rankInput = new RankAPIInput();
rankInput.setModel("jina-reranker-v2-base-multilingual");
rankInput.setQuery(new Query("What is machine learning?"));

// Create documents list
List<RankAPIInputDocumentsInner> documents = new ArrayList<>();
documents.add(new RankAPIInputDocumentsInner("Machine learning is a subset of AI"));
documents.add(new RankAPIInputDocumentsInner("The weather is nice today"));
documents.add(new RankAPIInputDocumentsInner("Neural networks are used in deep learning"));
rankInput.setDocuments(documents);
rankInput.setTopN(3);
rankInput.setReturnDocuments(true);

RankingOutput result = rerankApi.rankV1RerankPost(rankInput);
```

### Creating Multi-Vector Embeddings (ColBERT)

```java
import ai.pairsys.jina4j.client.ApiClient;
import ai.pairsys.jina4j.client.Configuration;
import ai.pairsys.jina4j.client.auth.HttpBearerAuth;
import ai.pairsys.jina4j.client.api.MultiVectorApi;
import ai.pairsys.jina4j.client.model.*;
import java.util.Arrays;
import java.util.List;

// Configure API client
ApiClient defaultClient = Configuration.getDefaultApiClient();

// Note: For multi-vector operations, increase timeout settings
defaultClient.setConnectTimeout(30000); // 30 seconds
defaultClient.setReadTimeout(60000); // 60 seconds
defaultClient.setWriteTimeout(60000); // 60 seconds

// Set your API key
HttpBearerAuth HTTPBearer = (HttpBearerAuth) defaultClient.getAuthentication("HTTPBearer");
HTTPBearer.setBearerToken("JINA_API_KEY");

// Create multi-vector API instance
MultiVectorApi multiVectorApi = new MultiVectorApi(defaultClient);
TextEmbeddingAPIInput multiVectorInput = new TextEmbeddingAPIInput();
multiVectorInput.setModel("jina-colbert-v1-en");

// Create input with list of strings
List<String> texts = Arrays.asList(
    "ColBERT generates multiple vectors per document",
    "Each token gets its own vector representation"
);
multiVectorInput.setInput(new Input(texts));
multiVectorInput.setInputType(TextEmbeddingAPIInput.InputTypeEnum.DOCUMENT);

ColbertModelEmbeddingsOutput result = multiVectorApi.createMultiVectorV1MultiVectorPost(multiVectorInput);
```

## API Endpoints

### Embeddings
- `POST /v1/embeddings` - Create embeddings for text or images
- `POST /v1/multi-vector` - Create multi-vector embeddings (ColBERT)

### Reranking
- `POST /v1/rerank` - Rerank documents by relevance

## Available Models

### Embedding Models
- `jina-embeddings-v4` - Latest and most powerful embedding model
- `jina-embeddings-v3` - Previous generation embedding model
- `jina-embeddings-v2-base-en` - English base model
- `jina-embeddings-v2-base-es` - Spanish base model
- `jina-embeddings-v2-base-de` - German base model
- `jina-embeddings-v2-base-zh` - Chinese base model
- `jina-embeddings-v2-base-code` - Code embedding model
- `jina-clip-v1` - Multimodal embeddings (text + images)
- `jina-clip-v2` - Improved multimodal embeddings
- `jina-colbert-v2` - ColBERT-style, multilingual
- `jina-colbert-v1-en` - ColBERT-style, English, 8k-token length

The following two embedding models are current inactive 
- `jina-code-embeddings-0.5b` - Smaller code embedding model
- `jina-code-embeddings-1.5b` - Larger code embedding model

### Reranking Models
- `jina-reranker-v2-base-multilingual` - Multilingual reranker
- `jina-reranker-v1-base-en` - English reranker
- `jina-reranker-v1-tiny-en` - Lightweight English reranker
- `jina-reranker-v1-turbo-en` - Fast English reranker
- `jina-colbert-v1-en` - ColBERT-style  v1
- `jina-colbert-v2` - ColBERT-style  v2, multilingual
- `jina-reranker-m0` - Multilingual, multimodal reranker

Note that the two ColBERT models are capable of both embedding and reranking functionality.


## Building from Source

```bash
# Clone the repository
git clone https://github.com/PAIR-Systems-Inc/jina4j.git
cd jina4j

# Generate client code from OpenAPI spec
./gradlew openApiGenerate

# Build the library
./gradlew build

# Publish to local Maven repository
./gradlew publishToMavenLocal
```

## License

This project is licensed under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions:
- Create an issue on [GitHub](https://github.com/PAIR-Systems-Inc/jina4j/issues)