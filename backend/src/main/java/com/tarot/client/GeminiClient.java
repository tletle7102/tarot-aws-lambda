package com.tarot.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tarot.model.TarotDeck.DrawnCard;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class GeminiClient {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String apiKey;

    public GeminiClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public String readTarot(String concern, List<DrawnCard> cards) throws Exception {
        String prompt = buildPrompt(concern, cards);

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        // System instruction
        JsonObject systemTextPart = new JsonObject();
        systemTextPart.addProperty("text", systemPrompt());

        JsonArray systemParts = new JsonArray();
        systemParts.add(systemTextPart);

        JsonObject systemInstruction = new JsonObject();
        systemInstruction.add("parts", systemParts);

        // Generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.9);
        generationConfig.addProperty("maxOutputTokens", 1024);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contents);
        requestBody.add("systemInstruction", systemInstruction);
        requestBody.add("generationConfig", generationConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error: " + response.statusCode() + " - " + response.body());
        }

        return extractText(response.body());
    }

    private String buildPrompt(String concern, List<DrawnCard> cards) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 질문자의 고민\n").append(concern).append("\n\n");
        sb.append("## 뽑은 카드 (3장 스프레드: 과거-현재-미래)\n");

        String[] positions = {"과거", "현재", "미래"};
        for (int i = 0; i < cards.size(); i++) {
            DrawnCard dc = cards.get(i);
            sb.append(String.format("%d. [%s] %s — %s\n",
                    i + 1, positions[i], dc.displayName(), dc.displayMeaning()));
        }

        sb.append("\n위 카드 배치를 기반으로 타로 리딩을 해주세요.");
        return sb.toString();
    }

    private String systemPrompt() {
        return """
                당신은 경험 많은 타로 마스터입니다. 따뜻하고 공감적인 어조로 타로 리딩을 제공합니다.

                규칙:
                1. 반드시 한국어로 답변하세요.
                2. 각 카드(과거/현재/미래)에 대해 질문자의 고민과 연결하여 해석하세요.
                3. 마지막에 종합 조언을 한 문단으로 제공하세요.
                4. 응답은 따뜻하되 구체적이어야 합니다. 너무 일반적인 말은 피하세요.
                5. 응답 형식:
                   🃏 과거: [카드명] - 해석
                   🃏 현재: [카드명] - 해석
                   🃏 미래: [카드명] - 해석
                   ✨ 종합 조언: ...
                """;
    }

    private String extractText(String responseBody) {
        JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
        return json.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();
    }
}
