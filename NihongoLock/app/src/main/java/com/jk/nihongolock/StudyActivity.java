package com.jk.nihongolock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class StudyActivity extends Activity {
    private StudyRepository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView timerText;
    private TextView stateText;
    private LinearLayout questionBox;
    private List<QuestionBank.Q> questions;
    private JapaneseSpeech speech;
    private int qIndex = 0;
    private int pendingSeconds = 0;
    private long lastInteraction = 0L;
    private boolean resumed = false;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (resumed && hasWindowFocus() && lastInteraction > 0
                    && SystemClock.elapsedRealtime() - lastInteraction <= 90_000L) {
                pendingSeconds++;
                if (pendingSeconds >= 5) flushPending();
            }
            updateStatus();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.darkSystemBars(this);
        repo = new StudyRepository(this);
        speech = new JapaneseSpeech(this);
        build();
        handler.post(tick);
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(16,17,20));
        LinearLayout root = Ui.column(this);
        scroll.addView(root);
        setContentView(scroll);

        root.addView(Ui.title(this, "오늘의 일본어"));
        timerText = Ui.text(this, "", 24, Color.WHITE);
        timerText.setTypeface(timerText.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(timerText);
        stateText = Ui.text(this,
                "문제 풀이·듣기·말하기 시간은 모두 오늘 20분에 합산됩니다. 화면을 실제로 사용한 시간만 인정되며 90초 이상 입력이 없으면 멈춥니다.",
                14, Color.LTGRAY);
        stateText.setPadding(0, 0, 0, Ui.dp(this, 12));
        root.addView(stateText);

        if (repo.canUsePass()) {
            Button pass = Ui.button(this, "PASS 사용하고 벌칙 종료");
            pass.setOnClickListener(v -> {
                markInteraction();
                flushPending();
                if (repo.usePass()) {
                    Toast.makeText(this, "PASS 사용 완료", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
            root.addView(pass);
        }

        questionBox = Ui.card(this);
        root.addView(questionBox);
        reloadQuestions();
        showQuestion();

        Button ai = Ui.button(this, "GPT 자유 첨삭으로 공부하기");
        ai.setOnClickListener(v -> {
            markInteraction();
            flushPending();
            startActivity(new Intent(this, AiPracticeActivity.class));
        });
        root.addView(ai);

        Button surrender = Ui.button(this, "공부 포기 · 앱 삭제");
        surrender.setOnClickListener(v -> {
            flushPending();
            Intent i = new Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        root.addView(surrender);
    }

    private void reloadQuestions() {
        questions = QuestionBank.forLevel(repo.getLevel(), repo.getRecentQuestionIds());
        if (questions.isEmpty()) questions = QuestionBank.forLevel(1);
        qIndex = 0;
    }

    private void showQuestion() {
        if (qIndex >= questions.size()) reloadQuestions();
        QuestionBank.Q q = QuestionBank.shuffled(questions.get(qIndex));
        repo.markQuestionSeen(q.id);
        questionBox.removeAllViews();
        questionBox.addView(Ui.text(this,
                QuestionBank.kindLabel(q.kind) + " · JLPT Lv." + q.level,
                13, Color.rgb(151, 182, 255)));
        RubyTextView prompt = Ui.rubyText(this, q.prompt, 20, Color.WHITE);
        prompt.setTextPadding(0, 0, 0, 8);
        questionBox.addView(prompt);
        Button listen = Ui.button(this, "🔊 듣기 (오늘 20분에 포함)");
        listen.setOnClickListener(v -> {
            markInteraction();
            speech.speak(q.audioText);
        });
        questionBox.addView(listen);
        Button speaking = Ui.button(this, "🎙 말하기 (오늘 20분에 포함)");
        speaking.setOnClickListener(v -> {
            markInteraction();
            flushPending();
            startActivity(new Intent(this, SpeakingActivity.class));
        });
        questionBox.addView(speaking);
        for (int i = 0; i < q.options.length; i++) {
            final int chosen = i;
            RubyTextView b = Ui.rubyButton(this, q.options[i]);
            b.setOnClickListener(v -> answer(q, chosen));
            questionBox.addView(b);
        }
    }

    private void answer(QuestionBank.Q q, int chosen) {
        markInteraction();
        boolean correct = chosen == q.answer;
        repo.recordAnswer(q, chosen, "study");
        Toast.makeText(this, (correct ? "정답 ✓  " : "오답 · ") + q.explanation, Toast.LENGTH_LONG).show();
        qIndex++;
        showQuestion();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN || ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            markInteraction();
        }
        return super.dispatchTouchEvent(ev);
    }

    private void markInteraction() {
        lastInteraction = SystemClock.elapsedRealtime();
    }

    private void flushPending() {
        if (pendingSeconds > 0) {
            int n = pendingSeconds;
            pendingSeconds = 0;
            repo.addStudySeconds(n);
        }
    }

    private void updateStatus() {
        int current = repo.getTodaySeconds() + pendingSeconds;
        int target = repo.getTargetSeconds();
        timerText.setText(StudyRepository.formatSeconds(current) + " / " + StudyRepository.formatSeconds(target));
        boolean active = lastInteraction > 0 && SystemClock.elapsedRealtime() - lastInteraction <= 90_000L;
        stateText.setText(active ? "● 학습시간 기록 중" : "Ⅱ 일시정지 · 화면을 터치하고 문제를 풀면 다시 기록됩니다.");

        if (current >= target) {
            flushPending();
            if (!repo.isPenaltyActive() && repo.getTodaySeconds() >= repo.getTargetSeconds()) {
                stateText.setText("✓ 오늘 목표를 완료했습니다. 이제 나가도 됩니다.");
            }
        }
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        repo.reconcile();
        updateStatus();
    }

    @Override protected void onPause() {
        flushPending();
        resumed = false;
        super.onPause();
    }

    @Override protected void onDestroy() {
        flushPending();
        handler.removeCallbacksAndMessages(null);
        if (speech != null) speech.shutdown();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        flushPending();
        if (repo.isPenaltyActive()) {
            Toast.makeText(this, "벌칙 시간에는 35분 완료, PASS 사용 또는 22:00까지 나갈 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}
