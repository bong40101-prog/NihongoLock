package com.jk.nihongolock;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private StudyRepository repo;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.darkSystemBars(this);
        repo = new StudyRepository(this);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        repo.reconcile();
        if (repo.isPenaltyActive()) {
            startActivity(new Intent(this, StudyActivity.class));
            return;
        }
        build();
        UpdateManager.maybeCheckOnLaunch(this);
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(16,17,20));
        root = Ui.column(this);
        scroll.addView(root);
        setContentView(scroll);

        root.addView(Ui.title(this, "ニホンゴ LOCK"));
        root.addView(Ui.text(this, "매일 20분. 놓치면 다음날 17:00~22:00에 35분.", 15, Color.LTGRAY));

        LinearLayout status = Ui.card(this);
        int today = repo.getTodaySeconds();
        int target = repo.getTargetSeconds();
        TextView progress = Ui.text(this,
                "오늘 학습  " + StudyRepository.formatSeconds(today) + " / " + StudyRepository.formatSeconds(target),
                21, Color.WHITE);
        progress.setTypeface(progress.getTypeface(), android.graphics.Typeface.BOLD);
        status.addView(progress);
        status.addView(Ui.text(this, "연속 성공  " + repo.getStreak() + "일", 16, Color.LTGRAY));
        status.addView(Ui.text(this, "PASS  " + repo.getPasses() + " / " + StudyRepository.MAX_PASSES, 16, Color.LTGRAY));
        status.addView(Ui.text(this, "현재 레벨  Lv." + repo.getLevel(), 16, Color.LTGRAY));

        String penalty;
        if (repo.isPenaltyActive()) penalty = "🔥 벌칙 진행 중 · 35분 완료 또는 22:00 해제";
        else if (repo.isPenaltyPending()) penalty = "⚠ 오늘 17:00부터 벌칙 예정 · 미리 35분 완료하면 면제";
        else if (repo.isPenaltyComplete()) penalty = "✓ 오늘 벌칙 학습 완료";
        else penalty = "✓ 현재 벌칙 없음";
        TextView p = Ui.text(this, penalty, 15, repo.isPenaltyToday() ? Color.rgb(255,107,107) : Color.rgb(101,209,138));
        p.setPadding(0, Ui.dp(this,10), 0, 0);
        status.addView(p);
        root.addView(status);

        Button study = Ui.button(this, repo.isPenaltyToday() && !repo.isPenaltyComplete() ? "35분 벌칙 학습 시작/계속" : "20분 학습 시작/계속");
        study.setOnClickListener(v -> startActivity(new Intent(this, StudyActivity.class)));
        root.addView(study);

        Button ai = Ui.button(this, "GPT 일본어 첨삭");
        ai.setOnClickListener(v -> startActivity(new Intent(this, AiPracticeActivity.class)));
        root.addView(ai);

        Button level = Ui.button(this, repo.isLevelTestDone() ? "레벨 테스트 다시 하기" : "처음 레벨 테스트 시작");
        level.setOnClickListener(v -> startActivity(new Intent(this, LevelTestActivity.class)));
        root.addView(level);

        if (repo.canUsePass()) {
            Button pass = Ui.button(this, "PASS 1장 사용해서 오늘 벌칙 면제");
            pass.setOnClickListener(v -> {
                if (repo.usePass()) {
                    Toast.makeText(this, "PASS를 사용했습니다.", Toast.LENGTH_SHORT).show();
                    build();
                }
            });
            root.addView(pass);
        }

        boolean access = isAccessibilityServiceEnabled();
        LinearLayout authCard = Ui.card(this);
        authCard.addView(Ui.text(this, "강제 학습 권한", 18, Color.WHITE));
        authCard.addView(Ui.text(this,
                access ? "✓ 접근성 서비스가 켜져 있습니다." : "꺼져 있음. 벌칙 중 다른 앱 차단을 위해 한 번 켜야 합니다.",
                14, access ? Color.rgb(101,209,138) : Color.rgb(255,180,80)));
        Button accessButton = Ui.button(this, access ? "접근성 설정 확인" : "접근성 설정 열기");
        accessButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        authCard.addView(accessButton);
        root.addView(authCard);

        Button settings = Ui.button(this, "API / 앱 설정");
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        root.addView(settings);

        TextView note = Ui.text(this,
                "앱을 삭제하면 접근성 서비스와 모든 제한이 함께 사라집니다. 다시 설치하면 기록/PASS는 처음부터 시작합니다.",
                13, Color.GRAY);
        note.setPadding(0, Ui.dp(this,18), 0, 0);
        root.addView(note);
    }

    private boolean isAccessibilityServiceEnabled() {
        String expected = new ComponentName(this, StudyAccessibilityService.class).flattenToString();
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            if (expected.equalsIgnoreCase(splitter.next())) return true;
        }
        return false;
    }
}
