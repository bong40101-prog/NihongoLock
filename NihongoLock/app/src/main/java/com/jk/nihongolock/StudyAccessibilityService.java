package com.jk.nihongolock;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StudyAccessibilityService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private StudyRepository repo;
    private WindowManager wm;
    private View overlay;
    private TextView status;
    private TextView detail;
    private String currentPackage = "";

    private static final Set<String> ALWAYS_ALLOWED = new HashSet<>(Arrays.asList(
            "com.skt.prod.dialer",                  // 에이닷 전화
            "com.samsung.android.incallui",        // Samsung in-call UI
            "com.android.incallui",
            "com.android.server.telecom",
            "com.android.systemui",               // lockscreen / emergency / system safety UI
            "com.android.phone",
            "com.samsung.android.dialer",
            "com.google.android.dialer",
            "com.sec.android.app.clockpackage",    // Samsung alarm/clock full-screen UI
            "com.samsung.android.emergency",
            "com.samsung.android.app.safetyassistance",
            "com.google.android.apps.safetyhub",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.samsung.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller"
    ));

    private final Runnable monitor = new Runnable() {
        @Override public void run() {
            evaluateBlock();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        repo = new StudyRepository(this);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler.removeCallbacksAndMessages(null);
        handler.post(monitor);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence pkg = event.getPackageName();
        if (pkg != null) currentPackage = pkg.toString();
        evaluateBlock();
    }

    private void evaluateBlock() {
        if (repo == null) repo = new StudyRepository(this);
        repo.reconcile();
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        boolean locked = km != null && km.isKeyguardLocked();

        boolean ownApp = getPackageName().equals(currentPackage);
        boolean allowed = ownApp || ALWAYS_ALLOWED.contains(currentPackage);
        boolean shouldBlock = repo.isPenaltyActive() && !locked && !allowed;

        if (shouldBlock) showOverlay();
        else hideOverlay();

        if (overlay != null) refreshOverlayText();
    }

    private void showOverlay() {
        if (overlay != null || wm == null) return;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(Ui.dp(this, 28), Ui.dp(this, 70), Ui.dp(this, 28), Ui.dp(this, 36));
        root.setBackgroundColor(Color.rgb(13,14,17));

        TextView title = Ui.centeredText(this, "🔥 일본어 벌칙 모드", 30);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        status = Ui.centeredText(this, "", 24);
        status.setPadding(0, Ui.dp(this, 26), 0, Ui.dp(this, 8));
        root.addView(status, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        detail = Ui.centeredText(this, "", 15);
        detail.setTextColor(Color.LTGRAY);
        detail.setPadding(0, 0, 0, Ui.dp(this, 24));
        root.addView(detail, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button study = Ui.button(this, "일본어 공부 시작 / 계속");
        study.setOnClickListener(v -> {
            hideOverlay();
            Intent i = new Intent(this, StudyActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        });
        root.addView(study);

        Button phone = Ui.button(this, "에이닷 전화 열기");
        phone.setOnClickListener(v -> launchAdotPhone());
        root.addView(phone);

        Button emergency = Ui.button(this, "전화 / 긴급전화 화면");
        emergency.setOnClickListener(v -> {
            try {
                Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(this, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(emergency);

        Button surrender = Ui.button(this, "공부 포기 · 앱 삭제");
        surrender.setOnClickListener(v -> {
            try {
                hideOverlay();
                Intent i = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(this, "삭제 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(surrender);

        if (repo.canUsePass()) {
            Button pass = Ui.button(this, "PASS 1장 사용 (보유 " + repo.getPasses() + ")");
            pass.setOnClickListener(v -> {
                if (repo.usePass()) {
                    Toast.makeText(this, "PASS 사용 · 벌칙 해제", Toast.LENGTH_SHORT).show();
                    hideOverlay();
                }
            });
            root.addView(pass);
        }

        TextView safety = Ui.centeredText(this,
                "전화 수신/발신과 긴급 기능은 항상 우선합니다.\n통화가 끝나면 벌칙이 남아 있을 경우 다시 이 화면으로 돌아옵니다.",
                13);
        safety.setTextColor(Color.GRAY);
        safety.setPadding(0, Ui.dp(this, 28), 0, 0);
        root.addView(safety);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.setTitle("NihongoLockPenalty");

        overlay = root;
        try {
            wm.addView(overlay, params);
            refreshOverlayText();
        } catch (Exception e) {
            overlay = null;
        }
    }

    private void refreshOverlayText() {
        if (status == null || detail == null) return;
        int seconds = repo.getTodaySeconds();
        status.setText(StudyRepository.formatSeconds(seconds) + " / " + StudyRepository.formatSeconds(StudyRepository.PENALTY_SECONDS));
        int left = repo.secondsUntilPenaltyEnd();
        int h = left / 3600;
        int m = (left % 3600) / 60;
        int s = left % 60;
        detail.setText("45분 완료 시 즉시 해제\n22:00 자동 해제 · 남은 벌칙창 " + String.format(java.util.Locale.KOREA, "%d:%02d:%02d", h, m, s));
    }

    private void hideOverlay() {
        if (overlay != null && wm != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        overlay = null;
        status = null;
        detail = null;
    }

    private void launchAdotPhone() {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage("com.skt.prod.dialer");
            if (i == null) i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            hideOverlay();
        } catch (Exception e) {
            Toast.makeText(this, "에이닷 전화를 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onInterrupt() {
        hideOverlay();
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        hideOverlay();
        super.onDestroy();
    }
}
