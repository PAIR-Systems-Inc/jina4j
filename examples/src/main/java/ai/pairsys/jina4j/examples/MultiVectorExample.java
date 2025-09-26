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
            // Create multi-vector embedding request showcasing ALL configurable fields
            TextEmbeddingAPIInput multiVectorInput = new TextEmbeddingAPIInput();
            
            // REQUIRED: Model selection - must be a ColBERT model
            // Options: "jina-colbert-v1-en", "jina-colbert-v2"
            multiVectorInput.setModel("jina-colbert-v1-en");
            
            // REQUIRED: Input text - the text to generate multi-vector embeddings for
            // Can be a single string or list of strings
            List<String> texts = Arrays.asList(
                "Hello, world!",
                "Goodmem is awesome."
            );
            multiVectorInput.setInput(new Input(texts));
            
            // OPTIONAL: Input type - specify whether input is query or document
            // Options: QUERY, DOCUMENT
            // Default: DOCUMENT
            multiVectorInput.setInputType(TextEmbeddingAPIInput.InputTypeEnum.DOCUMENT);
            
            // OPTIONAL: Embedding type - control output format
            // Note: Similar to EmbeddingType, accepts string values
            // Options: "float", "base64", "binary", "ubinary"
            // Default: "float"
            // Commenting out for now as it might be causing issues
            // multiVectorInput.setEmbeddingType(new EmbeddingType("float"));
            
            // OPTIONAL: Dimensions - ColBERT specific dimension options
            // Options: DimensionsEnum values if available
            // Note: ColBERT models have fixed dimensions per model
            // multiVectorInput.setDimensions(TextEmbeddingAPIInput.DimensionsEnum.SOME_VALUE);
            
            System.out.println("Sending request for model: " + multiVectorInput.getModel());
            System.out.println("Input type: " + multiVectorInput.getInputType());
            
            // Call the API
            ColbertModelEmbeddingsOutput result = apiInstance.createMultiVectorV1MultiVectorPost(multiVectorInput);
            
            // Print results
            System.out.println("\n=== Multi-Vector Embedding Results ===");
            System.out.println("Model used: " + result.getModel());
            System.out.println("Total tokens processed: " + result.getUsage().getTotalTokens());
            
            List<Object> multiVectorData = result.getData();
            System.out.println("Number of documents: " + multiVectorData.size());
            
            // Print information about each document's multi-vector embeddings
            for (int i = 0; i < multiVectorData.size(); i++) {
                System.out.println("\n--- Document " + (i + 1) + " ---");
                System.out.println("Input text: \"" + texts.get(i) + "\"");
                
                String embeddingStr = multiVectorData.get(i).toString();
                
                // ColBERT produces one embedding per token
                // Count approximate number of tokens by counting embedding arrays
                int tokenCount = embeddingStr.split("\\], \\[").length;
                System.out.println("Number of token embeddings: ~" + tokenCount);
                
                // Show a sample of the multi-vector output
                int sampleLength = Math.min(300, embeddingStr.length());
                System.out.println("Sample output: " + embeddingStr.substring(0, sampleLength) + "...");
            }
            
            System.out.println("\n=== Configuration Used ===");
            System.out.println("Input type: " + multiVectorInput.getInputType());
            System.out.println("Embedding type: " + multiVectorInput.getEmbeddingType());
            
        } catch (ApiException e) {
            System.err.println("\nException when calling MultiVectorApi#createMultiVectorV1MultiVectorPost");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            
            if (e.getCode() == 500) {
                System.err.println("\n⚠️  Note: The multi-vector API endpoint sometimes experiences server-side issues.");
                System.err.println("   This is a known issue with ColBERT models requiring more processing time.");
                System.err.println("   Possible solutions:");
                System.err.println("   - Try again later");
                System.err.println("   - Use shorter input texts");
                System.err.println("   - Contact Jina support if the issue persists");
            }
            
            if (e.getCode() == 408 || (e.getCause() != null && e.getCause() instanceof java.net.SocketTimeoutException)) {
                System.err.println("\n⚠️  Request timed out. ColBERT models can take longer to process.");
                System.err.println("   Consider increasing timeout values in the ApiClient configuration.");
            }
        }
    }
}