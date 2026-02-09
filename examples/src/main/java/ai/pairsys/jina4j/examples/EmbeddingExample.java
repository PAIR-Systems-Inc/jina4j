package ai.pairsys.jina4j.examples;

import ai.pairsys.jina4j.client.ApiClient;
import ai.pairsys.jina4j.client.ApiException;
import ai.pairsys.jina4j.client.Configuration;
import ai.pairsys.jina4j.client.api.SearchFoundationModelsApi;
import ai.pairsys.jina4j.client.auth.HttpBearerAuth;
import ai.pairsys.jina4j.client.model.BaseUsage;
import ai.pairsys.jina4j.client.model.EmbeddingRequest;
import ai.pairsys.jina4j.client.model.EmbeddingResponse;
import ai.pairsys.jina4j.client.model.EmbeddingType;
import ai.pairsys.jina4j.client.model.EmbeddingUsage;
import ai.pairsys.jina4j.client.model.EmbeddingsV3Request;
import ai.pairsys.jina4j.client.model.Input2AnyOfInner;
import ai.pairsys.jina4j.client.model.Input4;
import java.util.Arrays;
import java.util.List;

public class EmbeddingExample {
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
            List<Input2AnyOfInner> inputs = Arrays.asList(
                new Input2AnyOfInner("Hello, world!"),
                new Input2AnyOfInner("Goodmem.ai is awesome!"),
                new Input2AnyOfInner("The quick brown fox jumps over the lazy dog")
            );

            EmbeddingsV3Request request = new EmbeddingsV3Request();
            request.setModel(EmbeddingsV3Request.ModelEnum.JINA_EMBEDDINGS_V3);
            request.setInput(new Input4(inputs));
            request.setTask(EmbeddingsV3Request.TaskEnum.TEXT_MATCHING);
            request.setDimensions(512);
            request.setEmbeddingType(new EmbeddingType("float"));
            request.setNormalized(true);
            request.setLateChunking(false);
            request.setTruncate(true);

            EmbeddingResponse result = apiInstance.embeddingsV1EmbeddingsPost(new EmbeddingRequest(request));

            System.out.println("\n=== Embedding Results ===");
            System.out.println("Model used: " + result.getModel());

            Object usage = result.getUsage().getActualInstance();
            if (usage instanceof EmbeddingUsage) {
                EmbeddingUsage embeddingUsage = (EmbeddingUsage) usage;
                System.out.println("Total tokens: " + embeddingUsage.getTotalTokens());
                System.out.println("Prompt tokens: " + embeddingUsage.getPromptTokens());
            } else if (usage instanceof BaseUsage) {
                BaseUsage baseUsage = (BaseUsage) usage;
                System.out.println("Total tokens: " + baseUsage.getTotalTokens());
            }

            Object data = result.getData().getActualInstance();
            if (data instanceof List<?>) {
                System.out.println("Number of embedding results: " + ((List<?>) data).size());
            } else {
                System.out.println("Embedding data: " + data);
            }
        } catch (ApiException e) {
            System.err.println("Exception when calling SearchFoundationModelsApi#embeddingsV1EmbeddingsPost");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
