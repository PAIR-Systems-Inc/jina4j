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

Executable examples are provided in the [examples](examples/) directory. See the [examples README](examples/README.md) for detailed instructions on how to run them.

## API Endpoints

### Embeddings
- `POST /v1/embeddings` - Create embeddings for text or images
- `POST /v1/multi-vector` - Create multi-vector embeddings (ColBERT)

### Reranking
- `POST /v1/rerank` - Rerank documents by relevance

## Jina Models

Jina offers:
* Embedders
* Rerankers
* ColBERT-style models that can be used as both embedders and rerankers

### Embedders

Jina offers 3 kinds of embedders:
* Text-only embedders
* Multimodal embedders (text + images)
* Inactive according to Jina

#### Text-only embedding models
- `jina-embeddings-v3` - Previous generation embedding model
- `jina-embeddings-v2-base-en` - English base model
- `jina-embeddings-v2-base-es` - Spanish base model
- `jina-embeddings-v2-base-de` - German base model
- `jina-embeddings-v2-base-zh` - Chinese base model
- `jina-embeddings-v2-base-code` - Code embedding model

For text-only embedders, the `input` argument is a list of strings, like this 

```
curl https://api.jina.ai/v1/embeddings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer jina_xxxxxxx" \
  -d @- <<EOFEOF
  {
    "model": "jina-embeddings-v3",
    "task": "text-matching",
    "input": [
        "Hello, world!",
        "Goodmem is awesome!"
    ]
  }
EOFEOF
```

#### Multimodal embedding models (text + images)
- `jina-embeddings-v4` - Latest and most powerful embedding model
- `jina-clip-v1` - Multimodal embeddings (text + images)
- `jina-clip-v2` - Improved multimodal embeddings

For multimodal embedders, the `input` argument is a list of dictionaries whose keys are either `text` or `image` and values are text strings (if key is `text`) or image URLs/base64 strings (if key is `image`) like this:

```
curl https://api.jina.ai/v1/embeddings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer jina_xxxxxxx" \
  -d @- <<EOFEOF
  {
    "model": "jina-embeddings-v4",
    "task": "text-matching",
    "input": [
        {
            "text": "A beautiful sunset over the beach"
        },
        {
            "image": "https://i.ibb.co/nQNGqL0/beach1.jpg"
        },
        {
            "image": "iVBORw0KGgoAAAANSUhEUgAAABwAAAA4CAIAAABhUg/jAAAAMklEQVR4nO3MQREAMAgAoLkoFreTiSzhy4MARGe9bX99lEqlUqlUKpVKpVKpVCqVHksHaBwCA2cPf0cAAAAASUVORK5CYII="
        }
    ]
  }
EOFEOF
```

#### Inactive according to Jina 
- `jina-code-embeddings-0.5b` - Smaller code embedding model
- `jina-code-embeddings-1.5b` - Larger code embedding model


### Rerankers

Jina offers 2 kinds of rerankers:
* Text-only rerankers
* Multimodal rerankers (text + images) - NOT supported for now

#### Text-only rerankers
- `jina-reranker-v2-base-multilingual` - Multilingual reranker
- `jina-reranker-v1-base-en` - English reranker
- `jina-reranker-v1-turbo-en` - Fast English reranker
- `jina-reranker-v1-tiny-en` - Lightweight English reranker

#### NOT supported for now -- Multimodal rerankers (text + images)
- `jina-reranker-m0` - Multilingual, multimodal reranker

### ColBERT-style models that can be used as both a reranker and an embedder

Note: ColBERT-style models are not tested. 

Jina offers two ColBERT-style models that can be used for both reranking and multi-vector embedding:

- `jina-colbert-v2` - ColBERT-style, multilingual
- `jina-colbert-v1-en` - ColBERT-style, English, 8k-token length

Jina's ColBERT models operate only on text inputs.

To use the ColBERT models (`jina-colbert-v2`, `jina-colbert-v1-en`) for embedding, the request must be sent to the `multi-vector` endpoint instead of the `/embeddings` endpoint. 


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
- Jina resources: 
    - [Embedding API](https://jina.ai/embeddings/)
    - [Reranker API](https://jina.ai/reranker/)
    - [API-dashboard](https://jina.ai/api-dashboard)
    - [Open source Jina models on Hugging Face](https://huggingface.co/jinaai)
    - [OpenAPI spec](https://api.jina.ai/openapi.json)