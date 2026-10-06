package com.jk.nihongolock;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;

/** Keyboard-based Japanese writing practice. */
public class WritingActivity extends Activity {
    private StudyRepository repo;
    private JapaneseSpeech speech;
    private List<QuestionBank.Q> questions;
    private QuestionBank.Q current;
    private int index;
    private boolean checked;
    private TextView progress;
    private TextView feedback;
    private EditText input;
    private TextView timer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int pendingSeconds;
    private long lastInteraction;
    private boolean resumed;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (resumed && hasWindowFocus() && lastInteraction > 0
                    && SystemClock.elapsedRealtime() - lastInteraction <= 90_000L) {
                pendingSeconds++;
                if (pendingSeconds >= 5) flushPending();
            }
            updateTimer();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.darkSystemBars(this);
        repo = new StudyRepository(this);
        speech = new JapaneseSpeech(this);
        questions = QuestionBank.writingForLevel(repo.getLevel(), repo.getRecentQuestionIds());
        if (questions.isEmpty()) questions = QuestionBank.writingForLevel(repo.getLevel(), new HashSet<>());
        build();
        showQuestion();
        handler.post(tick);
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(16,17,20));
        LinearLayout root = Ui.column(this);
        scroll.addView(root);
        setContentView(scroll);

        root.addView(Ui.title(this, "쓰기 연습"));
        timer = Ui.text(this, "", 18, Color.WHITE);
        root.addView(timer);
        root.addView(Ui.text(this,
                "뜻을 보고 일본어를 키보드로 입력하세요. 한자까지 정확히 쓰는 연습입니다.",
                14, Color.LTGRAY));

        LinearLayout card = Ui.card(this);
        progress = Ui.text(this, "", 14, Color.rgb(151,182,255));
        card.addView(progress);
        TextView instruction = Ui.text(this, "", 20, Color.WHITE);
        instruction.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 8));
        card.addView(instruction);
        input = new EditText(this);
        input.setHint("일본어 입력");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setSingleLine(false);
        input.setMinLines(2);
        input.setGravity(android.view.Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) markInteraction(); });
        card.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 92)));

        Button check = Ui.button(this, "정답 확인");
        check.setOnClickListener(v -> checkAnswer());
        card.addView(check);
        Button listen = Ui.button(this, "🔊 정답 듣기");
        listen.setOnClickListener(v -> {
            markInteraction();
            if (current != null) speech.speak(current.writingAnswer);
        });
        card.addView(listen);
        feedback = Ui.text(this, "", 16, Color.LTGRAY);
        feedback.setPadding(0, Ui.dp(this, 10), 0, 0);
        card.addView(feedback);
        Button next = Ui.button(this, "다음 쓰기 문제");
        next.setOnClickListener(v -> {
            markInteraction();
            index = (index + 1) % questions.size();
            showQuestion();
        });
        card.addView(next);
        root.addView(card);

        // Keep the instruction reference in the view tree without a second field.
        instruction.setTag("writing_instruction");
    }

    private void showQuestion() {
        current = questions.get(index);
        repo.markQuestionSeen(current.id);
        checked = false;
        progress.setText((index + 1) + " / " + questions.size() + " · " + QuestionBank.kindLabel(current.kind));
        TextView instruction = findInstruction();
        if (instruction != null) instruction.setText(current.writingPrompt);
        input.setText("");
        feedback.setText("힌트가 필요하면 정답 듣기를 눌러 보세요.");
        feedback.setTextColor(Color.LTGRAY);
    }

    private TextView findInstruction() {
        if (input == null || !(input.getParent() instanceof LinearLayout)) return null;
        LinearLayout card = (LinearLayout) input.getParent();
        for (int i = 0; i < card.getChildCount(); i++) {
            android.view.View child = card.getChildAt(i);
            if ("writing_instruction".equals(child.getTag()) && child instanceof TextView) return (TextView) child;
        }
        return null;
    }

    private void checkAnswer() {
        markInteraction();
        if (checked) return;
        String typed = input.getText().toString().trim();
        if (typed.isEmpty()) {
            feedback.setText("일본어 답변을 입력해 주세요.");
            return;
        }
        checked = true;
        boolean correct = normalize(typed).equals(normalize(current.writingAnswer));
        repo.recordPracticeAnswer(current, "writing", typed, correct);
        if (correct) {
            feedback.setText("✓ 정답입니다! " + current.writingAnswer);
            feedback.setTextColor(Color.rgb(101,209,138));
        } else {
            feedback.setText("△ 정답: " + current.writingAnswer + "\n" + current.explanation);
            feedback.setTextColor(Color.rgb(255,180,80));
        }
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .replaceAll("[\\s\\p{Punct}。、！？「」『』・…]+", "");
    }

    private void markInteraction() { lastInteraction = SystemClock.elapsedRealtime(); }

    private void flushPending() {
        if (pendingSeconds > 0) {
            int seconds = pendingSeconds;
            pendingSeconds = 0;
            repo.addStudySeconds(seconds);
        }
    }

    private void updateTimer() {
        if (timer != null) {
            timer.setText("학습 " + StudyRepository.formatSeconds(repo.getTodaySeconds() + pendingSeconds)
                    + " / " + StudyRepository.formatSeconds(repo.getTargetSeconds()));
        }
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN || ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            markInteraction();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override protected void onResume() { super.onResume(); resumed = true; updateTimer(); }
    @Override protected void onPause() { flushPending(); resumed = false; super.onPause(); }
    @Override protected void onDestroy() {
        flushPending();
        handler.removeCallbacksAndMessages(null);
        if (speech != null) speech.shutdown();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        flushPending();
        if (repo.isPenaltyActive()) {
            android.widget.Toast.makeText(this, "벌칙 중에는 학습을 계속하거나 PASS를 사용해야 합니다.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}
