package com.jk.nihongolock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UpdateManager {
    private static final String PREFS = "update_settings_v1";
    private static final String K_REPO = "github_repo";
    private static final String K_LAST_CHECK = "last_check";
    private static final long AUTO_CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L;
    private static final String USER_AGENT = "NihongoLock-Android-Updater";

    private UpdateManager() {}

    public interface Callback {
        void onResult(UpdateInfo info, String error);
    }

    public static final class UpdateInfo {
        public final String versionName;
        public final String apkUrl;
        public final String notes;

        public UpdateInfo(String versionName, String apkUrl, String notes) {
            this.versionName = versionName;
            this.apkUrl = apkUrl;
            this.notes = notes == null ? "" : notes;
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getRepo(Context c) {
        return prefs(c).getString(K_REPO, "").trim();
    }

    public static void setRepo(Context c, String value) {
        String v = normalizeRepo(value);
        prefs(c).edit().putString(K_REPO, v).apply();
    }

    public static String normalizeRepo(String input) {
        if (input == null) return "";
        String s = input.trim();
        s = s.replace("https://github.com/", "");
        s = s.replace("http://github.com/", "");
        while (s.startsWith("/")) s = s.substring(1);
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".git")) s = s.substring(0, s.length() - 4);
        String[] p = s.split("/");
        if (p.length < 2) return "";
        return p[0] + "/" + p[1];
    }

    public static String currentVersionName(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return pi.versionName == null ? "0.0.0" : pi.versionName;
        } catch (Exception e) {
            return "0.0.0";
        }
    }

    public static void checkAsync(Context c, Callback callback) {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.execute(() -> {
            try {
                String repo = getRepo(c);
                if (repo.isEmpty()) throw new IllegalStateException("GitHub 업데이트 저장소가 설정되지 않았습니다. owner/repo 형식으로 입력하세요.");
                String api = "https://api.github.com/repos/" + repo + "/releases/latest";
                HttpURLConnection conn = (HttpURLConnection) new URL(api).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(25000);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                int code = conn.getResponseCode();
                String raw = readText(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
                if (code < 200 || code >= 300) throw new IllegalStateException("GitHub 응답 " + code + ": " + raw);

                JSONObject release = new JSONObject(raw);
                String tag = release.optString("tag_name", "").trim();
                String version = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
                String notes = release.optString("body", "");
                String apkUrl = "";
                JSONArray assets = release.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject a = assets.optJSONObject(i);
                        if (a == null) continue;
                        String name = a.optString("name", "").toLowerCase(Locale.ROOT);
                        if (name.endsWith(".apk")) {
                            apkUrl = a.optString("browser_download_url", "");
                            if (name.contains("nihongo") || name.contains("release")) break;
                        }
                    }
                }
                if (version.isEmpty()) throw new IllegalStateException("최신 Release의 버전 태그를 찾지 못했습니다.");
                if (apkUrl.isEmpty()) throw new IllegalStateException("최신 Release에서 APK 파일을 찾지 못했습니다.");
                prefs(c).edit().putLong(K_LAST_CHECK, System.currentTimeMillis()).apply();
                UpdateInfo info = isNewer(version, currentVersionName(c)) ? new UpdateInfo(version, apkUrl, notes) : null;
                callback.onResult(info, null);
            } catch (Exception e) {
                callback.onResult(null, e.getMessage());
            } finally {
                ex.shutdown();
            }
        });
    }

    public static void maybeCheckOnLaunch(Activity activity) {
        if (getRepo(activity).isEmpty()) return;
        long last = prefs(activity).getLong(K_LAST_CHECK, 0L);
        if (System.currentTimeMillis() - last < AUTO_CHECK_INTERVAL_MS) return;
        checkAsync(activity, (info, error) -> activity.runOnUiThread(() -> {
            if (info != null && !activity.isFinishing()) showUpdateDialog(activity, info);
        }));
    }

    public static void showUpdateDialog(Activity activity, UpdateInfo info) {
        String msg = "현재 " + currentVersionName(activity) + " → 새 버전 " + info.versionName;
        if (!info.notes.trim().isEmpty()) {
            String notes = info.notes.trim();
            if (notes.length() > 700) notes = notes.substring(0, 700) + "…";
            msg += "\n\n" + notes;
        }
        new AlertDialog.Builder(activity)
                .setTitle("새 업데이트 있음")
                .setMessage(msg)
                .setNegativeButton("나중에", null)
                .setPositiveButton("업데이트 설치", (d, w) -> downloadAndInstall(activity, info.apkUrl))
                .show();
    }

    public static void downloadAndInstall(Activity activity, String apkUrl) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.getPackageManager().canRequestPackageInstalls()) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(settings);
            android.widget.Toast.makeText(activity, "이 앱의 '알 수 없는 앱 설치'를 허용한 뒤 업데이트를 다시 눌러주세요.", android.widget.Toast.LENGTH_LONG).show();
            return;
        }

        android.widget.Toast.makeText(activity, "업데이트 APK 다운로드 중…", android.widget.Toast.LENGTH_SHORT).show();
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.execute(() -> {
            try {
                File apk = downloadToFile(activity, apkUrl);
                activity.runOnUiThread(() -> openSystemInstaller(activity, apk));
            } catch (Exception e) {
                activity.runOnUiThread(() -> android.widget.Toast.makeText(activity,
                        "업데이트 실패: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show());
            } finally {
                ex.shutdown();
            }
        });
    }

    private static File downloadToFile(Context c, String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(90000);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) throw new IllegalStateException("APK 다운로드 HTTP " + code);

        File dir = new File(c.getCacheDir(), "updates");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("업데이트 임시 폴더를 만들 수 없습니다.");
        File target = new File(dir, "update.apk");
        if (target.exists() && !target.delete()) throw new IllegalStateException("이전 업데이트 파일을 정리할 수 없습니다.");

        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            long total = 0L;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                total += n;
            }
            out.flush();
            if (total < 10_000L) throw new IllegalStateException("다운로드한 APK가 비정상적으로 작습니다.");
            return target;
        }
    }

    private static void openSystemInstaller(Activity activity, File apk) {
        Uri uri = Uri.parse("content://" + activity.getPackageName() + ".fileprovider/update.apk");
        Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(install);
        } catch (ActivityNotFoundException e) {
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, "application/vnd.android.package-archive");
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                activity.startActivity(view);
            } catch (Exception fallbackError) {
                android.widget.Toast.makeText(activity,
                        "APK 설치 화면을 열 수 없습니다.", android.widget.Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            android.widget.Toast.makeText(activity,
                    "APK 설치 화면을 열 수 없습니다: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private static boolean isNewer(String remote, String local) {
        int[] r = versionParts(remote);
        int[] l = versionParts(local);
        int n = Math.max(r.length, l.length);
        for (int i = 0; i < n; i++) {
            int rv = i < r.length ? r[i] : 0;
            int lv = i < l.length ? l[i] : 0;
            if (rv != lv) return rv > lv;
        }
        return false;
    }

    private static int[] versionParts(String v) {
        String clean = v == null ? "0" : v.replaceAll("[^0-9.]", "");
        String[] parts = clean.split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { out[i] = Integer.parseInt(parts[i]); }
            catch (Exception ignored) { out[i] = 0; }
        }
        return out;
    }

    private static String readText(InputStream stream) throws Exception {
        if (stream == null) return "";
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
