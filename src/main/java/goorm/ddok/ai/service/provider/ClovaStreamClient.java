package goorm.ddok.ai.service.provider;

import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 네이버 클로바 스튜디오 스트리밍 엔드포인트(SSE) 호출 클라이언트
 * - host: https://clovastudio.stream.ntruss.com
 * - path: /v3/chat-completions/{model}  (예: HCX-005)
 * - 인증: Authorization: Bearer {studio-api-key}
 */
@Component
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

        final String url = streamUrl.replaceAll("/+$", "") + "/v3/chat-completions/" + model;

        // 요청 바디
        JSONObject body = new JSONObject()
                .put("messages", new JSONArray()
                        .put(new JSONObject().put("role", "system").put("content", ""))
                        .put(new JSONObject().put("role", "user").put("content", prompt)))
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
        String lastFull = ""; // 누적(full) 조각 방지용

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) {
                throw new RuntimeException("Clova STREAM error: HTTP " + res.code());
            }

            BufferedSource source = res.body().source();

            // readUtf8Line()은 EOF에서 null을 리턴 → while(true) + null 체크
            while (true) {
                String line = source.readUtf8Line();
                if (line == null) break;             // 스트림 종료
                if (line.isBlank()) continue;         // keep-alive 공백 라인

                // SSE 포맷: "data: {...}"
                if (!line.startsWith("data:")) continue;

                String json = line.substring("data:".length()).trim();
                if ("[DONE]".equalsIgnoreCase(json)) break;

                try {
                    JSONObject chunk = new JSONObject(json);

                    // event 필드가 있으면 message/delta만 처리, 그 외(meta 등)는 무시
                    String event = chunk.optString("event", "message");
                    if (!event.equalsIgnoreCase("message") && !event.equalsIgnoreCase("delta")) {
                        continue;
                    }

                    // delta 우선, 없으면 message.content
                    String incoming = null;

                    JSONObject delta = chunk.optJSONObject("delta");
                    if (delta != null) {
                        incoming = delta.optString("content", "");
                    }

                    if (incoming == null || incoming.isBlank()) {
                        JSONObject msg = chunk.optJSONObject("message");
                        if (msg != null) {
                            incoming = msg.optString("content", "");
                        }
                    }

                    if (incoming == null || incoming.isBlank()) {
                        // 다른 스키마 대비: result/output/message.content 등
                        String fallback = chunk.optString("result",
                                chunk.optString("output",
                                        chunk.optJSONObject("message") != null
                                                ? chunk.optJSONObject("message").optString("content", "")
                                                : ""));
                        incoming = fallback;
                    }

                    if (incoming == null || incoming.isBlank()) continue;

                    // ===== 핵심: 누적(full) vs 증분(delta) 구분 =====
                    String current = combined.toString();

                    if (incoming.length() >= current.length() && incoming.startsWith(current)) {
                        // 누적(full) 조각 → 전체 대체 (중복 방지)
                        combined.setLength(0);
                        combined.append(incoming);
                        lastFull = incoming;
                    } else if (!incoming.equals(lastFull) && !current.endsWith(incoming)) {
                        // 진짜 증분(delta)로 판단 → append
                        combined.append(incoming);
                    }

                } catch (Exception ignore) {
                    // keepalive 등 JSON 아닐 수 있으므로 무시
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("CLOVA STREAM 호출 실패", e);
        }

        String out = combined.toString().trim();
        return out.isBlank() ? "[CLOVA 응답 없음]" : out;
    }
}