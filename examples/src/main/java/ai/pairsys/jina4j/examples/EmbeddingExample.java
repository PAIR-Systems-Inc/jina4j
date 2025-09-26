package ai.pairsys.jina4j.examples;

import ai.pairsys.jina4j.client.ApiClient;
import ai.pairsys.jina4j.client.ApiException;
import ai.pairsys.jina4j.client.Configuration;
import ai.pairsys.jina4j.client.auth.*;
import ai.pairsys.jina4j.client.model.*;
import ai.pairsys.jina4j.client.api.EmbeddingsApi;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class EmbeddingExample {
    public static void main(String[] args) {
        // Create API client
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        
        // Configure HTTP bearer authorization
        HttpBearerAuth HTTPBearer = (HttpBearerAuth) defaultClient.getAuthentication("HTTPBearer");
        String apiKey = System.getenv("JINA_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Please set JINA_API_KEY environment variable");
            System.exit(1);
        }
        HTTPBearer.setBearerToken(apiKey);
        
        // Create embeddings API instance
        EmbeddingsApi apiInstance = new EmbeddingsApi(defaultClient);
        
        try {
            // Create text embedding request showcasing ALL configurable fields
            TextEmbeddingInput textInput = new TextEmbeddingInput();
            
            // REQUIRED: Model selection
            // See README.md for list of supported models
            textInput.setModel("jina-embeddings-v3");
            
            // REQUIRED: Input text, the text to be embedded
            // Can be a single string, list of strings, or structured documents
            List<String> texts = Arrays.asList(
                "Hello, world!",
                "Goodmem.ai is awesome!",
                "The quick brown fox jumps over the lazy dog"
            );
            textInput.setInput(new Input(texts));
            
            // OPTIONAL: Task type - helps model produce better embeddings for your use case
            // Enum values: RETRIEVAL_QUERY, RETRIEVAL_PASSAGE, TEXT_MATCHING, CLASSIFICATION, SEPARATION
            // String values: "retrieval.query", "retrieval.passage", "text-matching", "classification", "separation"
            // Default: TEXT_MATCHING (if enum) or "text-matching" (if string)
            
            // Example using enum directly:
            // textInput.setTask(TextEmbeddingInput.TaskEnum.TEXT_MATCHING);
            
            // Example converting from string (if you have a string value):
            String taskString = "text-matching";
            textInput.setTask(TextEmbeddingInput.TaskEnum.fromValue(taskString));

            // OPTIONAL: Output dimensions - truncate embeddings to specified size
            // Set to smaller value to reduce memory usage
            textInput.setDimensions(32);
            
            // OPTIONAL: Embedding type - control output format
            // You can only use string here, because the original spec says it can be either a string or an arry of strings. It seems that the openAPI generator does not handle this case well.
            // "float", "base64", "binary", "ubinary"
            // Default: "float"
            // Example using string value:
            textInput.setEmbeddingType(new EmbeddingType("ubinary"));

            // OPTIONAL: Normalization - whether to normalize embeddings to unit L2 norm
            // Default: true (recommended for cosine similarity)
            textInput.setNormalized(true);
            
            // OPTIONAL: Late chunking - for processing long texts
            // When true, all sentences in input are concatenated and processed together
            // Default: false
            textInput.setLateChunking(false);
            
            // OPTIONAL: Truncate - whether to truncate text exceeding max token length
            // Default: false (will error if text is too long)
            // Set to true to automatically truncate long texts
            textInput.setTruncate(true);
            
            // Call the API
            EmbeddingInput input = new EmbeddingInput(textInput);
            ModelEmbeddingOutput result = apiInstance.createEmbeddingV1EmbeddingsPost(input);
            
            // Print results
            System.out.println("\n=== Embedding Results ===");
            System.out.println("Model used: " + result.getModel());
            System.out.println("Total tokens processed: " + result.getUsage().getTotalTokens());
            
            List<Object> embeddings = result.getData();
            System.out.println("Number of embeddings generated: " + embeddings.size());
            
            // Print information about each embedding
            for (int i = 0; i < embeddings.size(); i++) {
                System.out.println("\n--- Embedding " + (i + 1) + " ---");
                System.out.println("Input text: \"" + texts.get(i) + "\"");
                
                // The embedding data structure contains the actual vector
                String embeddingStr = embeddings.get(i).toString();
                
                // Show a sample of the embedding data
                String embeddingTypeValue = textInput.getEmbeddingType().getString();
                if ("float".equals(embeddingTypeValue) || "binary".equals(embeddingTypeValue) || "ubinary".equals(embeddingTypeValue)) {
                    // Extract and show first few values
                    if (embeddingStr.contains("embedding=")) {
                        int start = embeddingStr.indexOf("embedding=[") + 11;
                        int end = Math.min(start + 100, embeddingStr.length());
                        System.out.println("First few values: " + embeddingStr.substring(start, end) + "...");
                    }
                } else {
                    // base64 format
                    System.out.println("Embedding format: base64 encoded string");
                }
            }
            
            System.out.println("\n=== Configuration Used ===");
            System.out.println("Task: " + textInput.getTask());
            System.out.println("Requested dimensions: " + textInput.getDimensions());
            System.out.println("Embedding type: " + textInput.getEmbeddingType());
            System.out.println("Normalized: " + textInput.getNormalized());
            System.out.println("Late chunking: " + textInput.getLateChunking());
            System.out.println("Truncate: " + textInput.getTruncate());
            
        } catch (ApiException e) {
            System.err.println("Exception when calling EmbeddingsApi#createEmbeddingV1EmbeddingsPost");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}