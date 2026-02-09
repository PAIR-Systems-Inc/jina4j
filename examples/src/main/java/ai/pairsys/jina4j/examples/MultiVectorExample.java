package ai.pairsys.jina4j.examples;

import ai.pairsys.jina4j.client.ApiClient;
import ai.pairsys.jina4j.client.ApiException;
import ai.pairsys.jina4j.client.Configuration;
import ai.pairsys.jina4j.client.api.SearchFoundationModelsApi;
import ai.pairsys.jina4j.client.auth.HttpBearerAuth;
import ai.pairsys.jina4j.client.model.ColbertV2Request;
import ai.pairsys.jina4j.client.model.EmbeddingRequest;
import ai.pairsys.jina4j.client.model.EmbeddingResponse;
import ai.pairsys.jina4j.client.model.EmbeddingType;
import ai.pairsys.jina4j.client.model.Input2AnyOfInner;
import ai.pairsys.jina4j.client.model.Input4;
import java.util.Arrays;
import java.util.List;

public class MultiVectorExample {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setConnectTimeout(30000);
        defaultClient.setReadTimeout(60000);
        defaultClient.setWriteTimeout(60000);

        HttpBearerAuth httpBearer = (HttpBearerAuth) defaultClient.getAuthentication("HTTPBearer");
        String apiKey = System.getenv("JINA_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Please set JINA_API_KEY environment variable");
            System.exit(1);
        }
        httpBearer.setBearerToken(apiKey);

        SearchFoundationModelsApi apiInstance = new SearchFoundationModelsApi(defaultClient);

        try {
            List<Input2AnyOfInner> texts = Arrays.asList(
                new Input2AnyOfInner("Hello, world!"),
                new Input2AnyOfInner("Goodmem is awesome.")
            );

            ColbertV2Request request = new ColbertV2Request();
            request.setModel(ColbertV2Request.ModelEnum.JINA_COLBERT_V2);
            request.setInput(new Input4(texts));
            request.setInputType(ColbertV2Request.InputTypeEnum.DOCUMENT);
            request.setDimensions(ColbertV2Request.DimensionsEnum.NUMBER_128);
            request.setEmbeddingType(new EmbeddingType("float"));

            EmbeddingResponse result = apiInstance.embeddingsV1EmbeddingsPost(new EmbeddingRequest(request));

            System.out.println("\n=== Multi-Vector Embedding Results ===");
            System.out.println("Model used: " + result.getModel());
            System.out.println("Usage: " + result.getUsage().getActualInstance());
            System.out.println("Raw data payload type: " + result.getData().getActualInstance().getClass().getName());
            System.out.println("Raw data payload: " + result.getData().getActualInstance());
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchFoundationModelsApi#embeddingsV1EmbeddingsPost");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
        }
    }
}
