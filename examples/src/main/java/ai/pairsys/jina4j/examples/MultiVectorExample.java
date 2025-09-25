package ai.pairsys.jina4j.examples;

import ai.pairsys.jina4j.client.ApiClient;
import ai.pairsys.jina4j.client.ApiException;
import ai.pairsys.jina4j.client.Configuration;
import ai.pairsys.jina4j.client.auth.*;
import ai.pairsys.jina4j.client.model.*;
import ai.pairsys.jina4j.client.api.MultiVectorApi;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class MultiVectorExample {
    public static void main(String[] args) {
        // Create API client
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        
        // Increase timeout for multi-vector operations (they can take longer)
        defaultClient.setConnectTimeout(30000); // 30 seconds
        defaultClient.setReadTimeout(60000); // 60 seconds
        defaultClient.setWriteTimeout(60000); // 60 seconds
        
        // Configure HTTP bearer authorization
        HttpBearerAuth HTTPBearer = (HttpBearerAuth) defaultClient.getAuthentication("HTTPBearer");
        String apiKey = System.getenv("JINA_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Please set JINA_API_KEY environment variable");
            System.exit(1);
        }
        HTTPBearer.setBearerToken(apiKey);
        
        // Create multi-vector API instance
        MultiVectorApi apiInstance = new MultiVectorApi(defaultClient);
        
        try {
            // Create multi-vector embedding request (ColBERT)
            TextEmbeddingAPIInput multiVectorInput = new TextEmbeddingAPIInput();
            multiVectorInput.setModel("jina-colbert-v1-en");
            
            // Create input with list of strings
            List<String> texts = Arrays.asList(
                "Hello, world!",
                "Goodmem is awesome!"
            );
            multiVectorInput.setInput(new Input(texts));
            multiVectorInput.setInputType(TextEmbeddingAPIInput.InputTypeEnum.DOCUMENT);
            
            // Call the API
            ColbertModelEmbeddingsOutput result = apiInstance.createMultiVectorV1MultiVectorPost(multiVectorInput);
            
            // Print results
            System.out.println("Model: " + result.getModel());
            System.out.println("Usage: " + result.getUsage().getTotalTokens() + " tokens");
            
            List<Object> multiVectorData = result.getData();
            System.out.println("Number of documents: " + multiVectorData.size());
            
            for (int i = 0; i < multiVectorData.size(); i++) {
                System.out.println("\nDocument " + i + ":");
                // Note: Each document will have multiple embeddings (one per token)
                System.out.println(multiVectorData.get(i).toString().substring(0, Math.min(200, multiVectorData.get(i).toString().length())) + "...");
            }
            
        } catch (ApiException e) {
            System.err.println("Exception when calling MultiVectorApi#createMultiVectorV1MultiVectorPost");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}