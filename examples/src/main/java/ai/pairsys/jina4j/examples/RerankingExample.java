package ai.pairsys.jina4j.examples;

import ai.pairsys.jina4j.client.ApiClient;
import ai.pairsys.jina4j.client.ApiException;
import ai.pairsys.jina4j.client.Configuration;
import ai.pairsys.jina4j.client.auth.*;
import ai.pairsys.jina4j.client.model.*;
import ai.pairsys.jina4j.client.api.RerankApi;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class RerankingExample {
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
        
        // Create rerank API instance
        RerankApi apiInstance = new RerankApi(defaultClient);
        
        try {
            // Create rerank request
            RankAPIInput rankInput = new RankAPIInput();
            rankInput.setModel("jina-reranker-v2-base-multilingual");
            rankInput.setQuery(new Query("What is the capital of France?"));
            
            // Create documents list
            List<DocumentsInner> documents = new ArrayList<>();
            documents.add(new DocumentsInner("Madrid is the capital of Spain"));
            documents.add(new DocumentsInner("France is a country in Europe"));
            documents.add(new DocumentsInner("Paris to France is like Rome to Italy"));
            documents.add(new DocumentsInner("Berlin is the capital of Germany"));
            documents.add(new DocumentsInner("Paris is the capital and largest city of France"));
            rankInput.setDocuments(documents);
            rankInput.setTopN(3);
            rankInput.setReturnDocuments(true);
            
            // Call the API
            RankingOutput result = apiInstance.rankV1RerankPost(rankInput);
            
            // Print results
            System.out.println("Model: " + result.getModel());
            System.out.println("Usage: " + result.getUsage().getTotalTokens() + " tokens");
            
            List<Object> rankedDocs = result.getResults();
            System.out.println("\nTop " + rankedDocs.size() + " ranked documents:");
            
            for (int i = 0; i < rankedDocs.size(); i++) {
                // Note: You'll need to cast and extract the document data based on the actual structure
                System.out.println("\nRank " + (i + 1) + ":");
                System.out.println(rankedDocs.get(i).toString());
            }
            
        } catch (ApiException e) {
            System.err.println("Exception when calling RerankApi#rankV1RerankPost");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
