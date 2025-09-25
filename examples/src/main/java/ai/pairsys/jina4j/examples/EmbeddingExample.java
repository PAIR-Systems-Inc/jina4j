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
            // Create text embedding request
            TextEmbeddingInput textInput = new TextEmbeddingInput();
            textInput.setModel("jina-embeddings-v3");
            
            // Create input with list of strings
            List<String> texts = Arrays.asList(
                "Hello, world!",
                "This is a test of Jina embeddings",
                "Text embeddings are powerful for semantic search"
            );
            textInput.setInput(new Input(texts));
            textInput.setTask(TextEmbeddingInput.TaskEnum.RETRIEVAL_PASSAGE);
            textInput.setDimensions(1024);
            
            // Call the API
            EmbeddingInput input = new EmbeddingInput(textInput);
            ModelEmbeddingOutput result = apiInstance.createEmbeddingV1EmbeddingsPost(input);
            
            // Print results
            System.out.println("Model: " + result.getModel());
            System.out.println("Usage: " + result.getUsage().getTotalTokens() + " tokens");
            
            List<Object> embeddings = result.getData();
            System.out.println("Number of embeddings: " + embeddings.size());
            
            // Print first few dimensions of each embedding
            for (int i = 0; i < embeddings.size(); i++) {
                System.out.println("\nText " + i + ": First 5 dimensions of embedding");
                // Note: You'll need to cast and extract the embedding data based on the actual structure
                System.out.println(embeddings.get(i).toString().substring(0, Math.min(100, embeddings.get(i).toString().length())) + "...");
            }
            
        } catch (ApiException e) {
            System.err.println("Exception when calling EmbeddingsApi#createEmbeddingV1EmbeddingsPost");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}