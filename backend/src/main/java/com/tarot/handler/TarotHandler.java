package com.tarot.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tarot.client.GeminiClient;
import com.tarot.model.TarotDeck;
import com.tarot.model.TarotDeck.DrawnCard;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TarotHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final Gson GSON = new Gson();
    private static final String TABLE_NAME = "tarot-readings";
    private static final String CORS_ORIGIN = System.getenv("CORS_ORIGIN") != null
            ? System.getenv("CORS_ORIGIN") : "*";

    private final GeminiClient geminiClient;
    private final DynamoDbClient dynamoDb;

    public TarotHandler() {
        String apiKey = fetchApiKey();
        this.geminiClient = new GeminiClient(apiKey);
        this.dynamoDb = DynamoDbClient.builder().region(Region.AP_NORTHEAST_2).build();
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        String method = getHttpMethod(event);
        String path = event.getRawPath() != null ? event.getRawPath() : "";

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return corsResponse(200, "");
        }

        try {
            // GET /reading/{id} — 저장된 결과 조회
            if ("GET".equalsIgnoreCase(method) && path.startsWith("/reading/")) {
                String id = path.substring("/reading/".length());
                return getReading(id);
            }

            // POST /reading — 새 리딩
            return createReading(event, context);

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return corsResponse(500, errorJson("타로 리딩 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    private APIGatewayV2HTTPResponse createReading(APIGatewayV2HTTPEvent event, Context context) throws Exception {
        String body = event.getBody();
        if (body == null || body.isBlank()) {
            return corsResponse(400, errorJson("고민 내용을 입력해주세요."));
        }

        JsonObject input = GSON.fromJson(body, JsonObject.class);
        String concern = input.has("concern") ? input.get("concern").getAsString().trim() : "";

        if (concern.isEmpty() || concern.length() > 500) {
            return corsResponse(400, errorJson("고민은 1~500자 사이로 입력해주세요."));
        }

        if (!input.has("cardIds") || input.getAsJsonArray("cardIds").size() != 3) {
            return corsResponse(400, errorJson("카드를 3장 선택해주세요."));
        }
        List<Integer> cardIds = new ArrayList<>();
        input.getAsJsonArray("cardIds").forEach(e -> cardIds.add(e.getAsInt()));
        List<DrawnCard> cards = TarotDeck.drawByIds(cardIds);

        String reading = geminiClient.readTarot(concern, cards);

        // 응답 구성
        JsonObject result = buildResultJson(cards, reading);

        // DynamoDB에 저장
        String id = UUID.randomUUID().toString().substring(0, 8);
        saveReading(id, GSON.toJson(result));
        result.addProperty("shareId", id);

        return corsResponse(200, GSON.toJson(result));
    }

    private APIGatewayV2HTTPResponse getReading(String id) {
        Map<String, AttributeValue> key = Map.of("id", AttributeValue.fromS(id));
        var response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(TABLE_NAME).key(key).build());

        if (!response.hasItem() || response.item().isEmpty()) {
            return corsResponse(404, errorJson("해당 리딩 결과를 찾을 수 없습니다."));
        }

        String data = response.item().get("data").s();
        return corsResponse(200, data);
    }

    private void saveReading(String id, String jsonData) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS(id));
        item.put("data", AttributeValue.fromS(jsonData));
        item.put("ttl", AttributeValue.fromN(
                String.valueOf(System.currentTimeMillis() / 1000 + 60 * 60 * 24 * 30))); // 30일 후 삭제

        dynamoDb.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
    }

    private JsonObject buildResultJson(List<DrawnCard> cards, String reading) {
        JsonObject result = new JsonObject();
        result.addProperty("reading", reading);

        var cardsArray = new com.google.gson.JsonArray();
        String[] positions = {"past", "present", "future"};
        for (int i = 0; i < cards.size(); i++) {
            DrawnCard dc = cards.get(i);
            JsonObject cardJson = new JsonObject();
            cardJson.addProperty("position", positions[i]);
            cardJson.addProperty("id", dc.card().id());
            cardJson.addProperty("name", dc.card().nameKo());
            cardJson.addProperty("nameEn", dc.card().name());
            cardJson.addProperty("reversed", dc.reversed());
            cardJson.addProperty("meaning", dc.displayMeaning());
            cardsArray.add(cardJson);
        }
        result.add("cards", cardsArray);
        return result;
    }

    private String getHttpMethod(APIGatewayV2HTTPEvent event) {
        if (event.getRequestContext() != null && event.getRequestContext().getHttp() != null) {
            return event.getRequestContext().getHttp().getMethod();
        }
        return "POST";
    }

    private APIGatewayV2HTTPResponse corsResponse(int statusCode, String body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Access-Control-Allow-Origin", CORS_ORIGIN);
        headers.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type");

        APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
        response.setStatusCode(statusCode);
        response.setHeaders(headers);
        response.setBody(body);
        return response;
    }

    private String errorJson(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        return GSON.toJson(error);
    }

    private static String fetchApiKey() {
        try (SsmClient ssm = SsmClient.builder().region(Region.AP_NORTHEAST_2).build()) {
            return ssm.getParameter(GetParameterRequest.builder()
                    .name("/tarot/gemini-api-key")
                    .withDecryption(true)
                    .build()).parameter().value();
        }
    }
}
