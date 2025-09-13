package goorm.ddok.ai.service.provider;

import lombok.RequiredArgsConstructor;
import okhttp3.*;
import okio.BufferedSource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Primary
@RequiredArgsConstructor
public class ClovaStreamClient implements AiModelClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient http = new OkHttpClient();

    @Value("${ai.clova.streamUrl:https://clovastudio.stream.ntruss.com}")
    private String streamUrl;

    @Value("${ai.clova.model:HCX-005}")
    private String model;

    @Value("${ai.clova.studio-api-key}")
    private String studioApiKey;

    @Value("${ai.maxTokens:800}")   private int defaultMaxTokens;
    @Value("${ai.temperature:0.7}") private double temperature;
    @Value("${ai.topP:0.8}")        private double topP;
    @Value("${ai.topK:0}")          private int topK;

    @Override
    public String generate(String prompt, int maxTokens) {
        if (studioApiKey == null || studioApiKey.isBlank()) {
            return "[CLOVA STREAM 비활성화] studio-api-key 미설정";
        }

        String url = streamUrl.replaceAll("/+$", "") + "/v3/chat-completions/" + model;

        JSONObject body = new JSONObject()
                .put("messages", new JSONArray()
                        .put(new JSONObject().put("role","system").put("content",""))
                        .put(new JSONObject().put("role","user").put("content", prompt)))
                .put("topP", topP)
                .put("topK", topK)
                .put("maxTokens", maxTokens > 0 ? maxTokens : defaultMaxTokens)
                .put("temperature", temperature)
                .put("repetitionPenalty", 1.1)
                .put("stop", new JSONArray())
                .put("seed", 0)
                .put("includeAiFilters", true);

        Request req = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + studioApiKey)
                .header("X-NCP-CLOVASTUDIO-REQUEST-ID", UUID.randomUUID().toString())
                .header("Accept", "text/event-stream")
                .header("Content-Type", "application/json; charset=utf-8")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        StringBuilder combined = new StringBuilder();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) {
                throw new RuntimeException("Clova STREAM error: HTTP " + res.code());
            }
            BufferedSource source = res.body().source();

            while (!source.exhausted()) {
                String line = source.readUtf8Line(); // null 허용
                if (line == null || line.isBlank()) continue;
                if (!line.startsWith("data:")) continue;

                String json = line.substring("data:".length()).trim();
                if ("[DONE]".equalsIgnoreCase(json)) break;

                try {
                    JSONObject chunk = new JSONObject(json);

                    // 우선 message/finishReason 중심으로 판단
                    JSONObject msg = chunk.optJSONObject("message");
                    String finish = chunk.has("finishReason")
                            ? chunk.optString("finishReason", null)
                            : (chunk.optJSONObject("result") != null
                            ? chunk.optJSONObject("result").optString("finishReason", null)
                            : null);

                    if (msg != null) {
                        String content = msg.optString("content", "");
                        if (finish == null || finish.isBlank()) {
                            // delta: 누적
                            if (!content.isBlank()) combined.append(content);
                        } else {
                            // final: 완성본으로 교체 후 종료
                            if (!content.isBlank()) {
                                combined.setLength(0);
                                combined.append(content);
                            }
                            break;
                        }
                        continue;
                    }

                    // event=result 류 스키마 대응(최종 한 번에 도착)
                    String event = chunk.optString("event", "");
                    if ("result".equalsIgnoreCase(event) || chunk.optJSONObject("result") != null) {
                        JSONObject result = chunk.optJSONObject("result");
                        String content = null;
                        if (result != null && result.optJSONObject("message") != null) {
                            content = result.getJSONObject("message").optString("content", null);
                        } else {
                            // 특수 스키마(문자열로 담기는 경우) 방지
                            content = chunk.optString("output", chunk.optString("result", null));
                        }
                        if (content != null && !content.isBlank()) {
                            combined.setLength(0);
                            combined.append(content);
                        }
                        break;
                    }

                    // 그 외 keepalive 등은 무시
                } catch (Exception ignore) {
                    // 비 JSON 라인 무시
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("CLOVA STREAM 호출 실패", e);
        }

        String out = combined.toString().trim();
        return out.isBlank() ? "[CLOVA 응답 없음]" : out;
    }
}