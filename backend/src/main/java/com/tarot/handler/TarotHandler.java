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
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TarotHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final Gson GSON = new Gson();
    private static final String CORS_ORIGIN = System.getenv("CORS_ORIGIN") != null
            ? System.getenv("CORS_ORIGIN") : "*";

    private final GeminiClient geminiClient;

    public TarotHandler() {
        String apiKey = fetchApiKey();
        this.geminiClient = new GeminiClient(apiKey);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(getHttpMethod(event))) {
            return corsResponse(200, "");
        }

        try {
            String body = event.getBody();
            if (body == null || body.isBlank()) {
                return corsResponse(400, errorJson("고민 내용을 입력해주세요."));
            }

            JsonObject input = GSON.fromJson(body, JsonObject.class);
            String concern = input.has("concern") ? input.get("concern").getAsString().trim() : "";

            if (concern.isEmpty() || concern.length() > 500) {
                return corsResponse(400, errorJson("고민은 1~500자 사이로 입력해주세요."));
            }

            // 카드 3장 뽑기
            List<DrawnCard> cards = TarotDeck.drawCards(3);

            // Gemini API 호출
            String reading = geminiClient.readTarot(concern, cards);

            // 응답 구성
            JsonObject result = new JsonObject();
            result.addProperty("reading", reading);

            var cardsArray = new com.google.gson.JsonArray();
            String[] positions = {"past", "present", "future"};
            for (int i = 0; i < cards.size(); i++) {
                DrawnCard dc = cards.get(i);
                JsonObject cardJson = new JsonObject();
                cardJson.addProperty("position", positions[i]);
                cardJson.addProperty("name", dc.card().nameKo());
                cardJson.addProperty("nameEn", dc.card().name());
                cardJson.addProperty("reversed", dc.reversed());
                cardJson.addProperty("meaning", dc.displayMeaning());
                cardsArray.add(cardJson);
            }
            result.add("cards", cardsArray);

            return corsResponse(200, GSON.toJson(result));

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return corsResponse(500, errorJson("타로 리딩 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
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
        headers.put("Access-Control-Allow-Methods", "POST, OPTIONS");
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
