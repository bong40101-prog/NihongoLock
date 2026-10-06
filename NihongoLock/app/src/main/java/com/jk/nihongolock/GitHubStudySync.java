package com.jk.nihongolock;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Debounced GitHub Contents API backup for answer history and study progress. */
public final class GitHubStudySync {
    private static final String PREFS = "github_sync_settings_v1";
    private static final String K_REPO = "repo";
    private static final String K_LAST_UPLOAD = "last_upload";
    private static final String K_LAST_ERROR = "last_error";
    private static final String K_PENDING = "pending";
    private static final String DEFAULT_REPO = "bong40101-prog/NihongoLock";
    private static final String FILE_PATH = "data/study-record.json";
    private static final String USER_AGENT = "NihongoLock-Android-StudySync";

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final Object LOCK = new Object();
    private static ScheduledFuture<?> pending;

    public interface Callback {
        void onResult(boolean success, String message);
    }

    private GitHubStudySync() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getRepo(Context context) {
        String saved = prefs(context).getString(K_REPO, "").trim();
        if (!saved.isEmpty()) return UpdateManager.normalizeRepo(saved);
        String updateRepo = UpdateManager.getRepo(context);
        return updateRepo.isEmpty() ? DEFAULT_REPO : updateRepo;
    }

    public static void setRepo(Context context, String value) {
        prefs(context).edit().putString(K_REPO, UpdateManager.normalizeRepo(value)).apply();
    }

    public static boolean isConfigured(Context context) {
        return !getRepo(context).isEmpty() && new GitHubTokenStore(context).get() != null;
    }

    public static String lastUpload(Context context) {
        long timestamp = prefs(context).getLong(K_LAST_UPLOAD, 0L);
        String error = prefs(context).getString(K_LAST_ERROR, "");
        String uploaded = timestamp == 0L ? "아직 성공한 업로드 없음"
                : java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(timestamp));
        if (!error.isEmpty()) return uploaded + " · 재시도 대기: " + error;
        if (prefs(context).getBoolean(K_PENDING, false)) return uploaded + " · 업로드 대기 중";
        return uploaded;
    }

    /** Uploads shortly after the latest answer/time update, avoiding one API commit per tap. */
    public static void schedule(Context context) {
        if (!isConfigured(context)) return;
        Context app = context.getApplicationContext();
        prefs(app).edit().putBoolean(K_PENDING, true).apply();
        synchronized (LOCK) {
            if (pending != null) pending.cancel(false);
            pending = EXECUTOR.schedule(() -> {
                try {
                    upload(app);
                } catch (Exception e) {
                    rememberFailure(app, e);
                    // Background backup must never block or interrupt studying.
                }
            }, 8, TimeUnit.SECONDS);
        }
    }

    public static void syncNow(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                upload(app);
                if (callback != null) callback.onResult(true, "학습 기록을 GitHub에 업로드했습니다.");
            } catch (Exception e) {
                rememberFailure(app, e);
                if (callback != null) callback.onResult(false, "업로드 실패: " + e.getMessage());
            }
        });
    }

    private static void upload(Context context) throws Exception {
        String repo = getRepo(context);
        String token = new GitHubTokenStore(context).get();
        if (repo.isEmpty()) throw new IllegalStateException("GitHub 저장소를 owner/repo 형식으로 설정해 주세요.");
        if (token == null || token.trim().isEmpty()) throw new IllegalStateException("GitHub 토큰을 먼저 저장해 주세요.");

        String api = "https://api.github.com/repos/" + repo + "/contents/" + FILE_PATH;
        String sha = findExistingSha(api, token);
        String encoded = Base64.encodeToString(
                new StudyRepository(context).exportJson().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        JSONObject body = new JSONObject();
        body.put("message", "자동 학습 기록 업데이트 " + LocalDate.now());
        body.put("content", encoded);
        if (sha != null && !sha.isEmpty()) body.put("sha", sha);

        HttpURLConnection conn = (HttpURLConnection) new URL(api).openConnection();
        try {
            conn.setRequestMethod("PUT");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream out = conn.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            String response = readText(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("GitHub 응답 " + code + responseSuffix(response));
            }
            prefs(context).edit()
                    .putLong(K_LAST_UPLOAD, System.currentTimeMillis())
                    .putBoolean(K_PENDING, false)
                    .remove(K_LAST_ERROR)
                    .apply();
        } finally {
            conn.disconnect();
        }
    }

    private static String findExistingSha(String api, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(api).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(25000);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            int code = conn.getResponseCode();
            String response = readText(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return null;
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("GitHub 조회 응답 " + code + responseSuffix(response));
            }
            return new JSONObject(response).optString("sha", "");
        } finally {
            conn.disconnect();
        }
    }

    private static String responseSuffix(String response) {
        if (response == null || response.trim().isEmpty()) return "";
        String compact = response.replace('\n', ' ').trim();
        return compact.length() > 220 ? ": " + compact.substring(0, 220) + "…" : ": " + compact;
    }

    private static void rememberFailure(Context context, Exception error) {
        String message = error == null ? "알 수 없는 오류" : error.getMessage();
        if (message == null || message.trim().isEmpty()) message = error.getClass().getSimpleName();
        if (message.length() > 180) message = message.substring(0, 180) + "…";
        prefs(context).edit().putBoolean(K_PENDING, true).putString(K_LAST_ERROR, message).apply();
    }

    private static String readText(InputStream stream) throws Exception {
        if (stream == null) return "";
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
