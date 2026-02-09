package ai.pairsys.jina4j.examples;

import ai.pairsys.jina4j.client.ApiClient;
import ai.pairsys.jina4j.client.ApiException;
import ai.pairsys.jina4j.client.Configuration;
import ai.pairsys.jina4j.client.api.SearchFoundationModelsApi;
import ai.pairsys.jina4j.client.auth.HttpBearerAuth;
import ai.pairsys.jina4j.client.model.Input2AnyOfInner;
import ai.pairsys.jina4j.client.model.RerankingRequest;
import ai.pairsys.jina4j.client.model.RerankingResponse;
import ai.pairsys.jina4j.client.model.RerankingResult;
import ai.pairsys.jina4j.client.model.TextRerankerRequest;
import java.util.Arrays;
import java.util.List;

public class RerankingExample {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        HttpBearerAuth httpBearer = (HttpBearerAuth) defaultClient.getAuthentication("HTTPBearer");

        String apiKey = System.getenv("JINA_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Please set JINA_API_KEY environment variable");
            System.exit(1);
        }
        httpBearer.setBearerToken(apiKey);

        SearchFoundationModelsApi apiInstance = new SearchFoundationModelsApi(defaultClient);

        try {
            List<Input2AnyOfInner> documents = Arrays.asList(
                new Input2AnyOfInner("Madrid is the capital of Spain"),
                new Input2AnyOfInner("France is a country in Europe"),
                new Input2AnyOfInner("Paris to France is like Rome to Italy"),
                new Input2AnyOfInner("Berlin is the capital of Germany"),
                new Input2AnyOfInner("Paris is the capital and largest city of France")
            );

            TextRerankerRequest rerankerRequest = new TextRerankerRequest();
            rerankerRequest.setModel(TextRerankerRequest.ModelEnum.JINA_RERANKER_V2_BASE_MULTILINGUAL);
            rerankerRequest.setQuery("What is the capital of France?");
            rerankerRequest.setDocuments(documents);
            rerankerRequest.setTopN(3);
            rerankerRequest.setReturnDocuments(true);

            RerankingResponse result = apiInstance.rerankV1RerankPost(new RerankingRequest(rerankerRequest));

            System.out.println("Model: " + result.getModel());
            System.out.println("Usage: " + result.getUsage().getTotalTokens() + " tokens");
            System.out.println("\nTop " + result.getResults().size() + " ranked documents:");

            for (RerankingResult ranked : result.getResults()) {
                System.out.println(
                    "Index=" + ranked.getIndex() +
                    ", score=" + ranked.getRelevanceScore() +
                    ", document=" + ranked.getDocument()
                );
            }
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchFoundationModelsApi#rerankV1RerankPost");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
