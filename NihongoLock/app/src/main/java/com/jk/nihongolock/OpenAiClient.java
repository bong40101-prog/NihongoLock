package com.jk.nihongolock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OpenAiClient {
    private final ApiKeyStore store;

    public OpenAiClient(ApiKeyStore store) {
        this.store = store;
    }

    public String ask(String instructions, String input) throws Exception {
        String key = store.getApiKey();
        if (key == null || key.isEmpty()) throw new IllegalStateException("OpenAI API 키가 없습니다.");

        JSONObject body = new JSONObject();
        body.put("model", store.getModel());
        body.put("instructions", instructions);
        body.put("input", input);
        body.put("max_output_tokens", 600);
        body.put("store", false);

        URL url = new URL("https://api.openai.com/v1/responses");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(45000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + key);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String raw = readAll(stream);
        if (code < 200 || code >= 300) {
            String message = raw;
            try {
                JSONObject err = new JSONObject(raw).optJSONObject("error");
                if (err != null) message = err.optString("message", raw);
            } catch (Exception ignored) {}
            throw new IllegalStateException("OpenAI 오류 " + code + ": " + message);
        }
        return extractOutputText(new JSONObject(raw));
    }

    private String extractOutputText(JSONObject response) {
        String direct = response.optString("output_text", "");
        if (!direct.isEmpty()) return direct;
        JSONArray output = response.optJSONArray("output");
        if (output == null) return "응답 텍스트를 찾지 못했습니다.";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < output.length(); i++) {
            JSONObject item = output.optJSONObject(i);
            if (item == null) continue;
            JSONArray content = item.optJSONArray("content");
            if (content == null) continue;
            for (int j = 0; j < content.length(); j++) {
                JSONObject c = content.optJSONObject(j);
                if (c == null) continue;
                if ("output_text".equals(c.optString("type"))) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(c.optString("text", ""));
                }
            }
        }
        return sb.length() == 0 ? "응답 텍스트를 찾지 못했습니다." : sb.toString();
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }
}
