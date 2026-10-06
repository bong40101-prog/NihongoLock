package com.jk.nihongolock;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiPracticeActivity extends Activity {
    private static final String[] PROMPTS = {
            "친구들과 이자카야에 가서 맥주를 마셨습니다.",
            "다음 달에 일본으로 여행을 갑니다. 맛있는 현지 음식을 먹고 싶습니다.",
            "호텔에 조금 일찍 도착했습니다. 얼리 체크인이 가능한지 물어보세요.",
            "식당에서 추천 메뉴와 인기 있는 술을 물어보세요.",
            "일본어를 아직 잘 못하지만 여행에서 불편하지 않을 정도로 말하고 싶습니다.",
            "친구에게 이번 주말 시간이 있으면 같이 술 한잔하자고 제안하세요.",
            "사진을 찍어도 되는지 정중하게 물어보세요.",
            "일정이 변경되어 친구에게 다시 연락한다고 말해보세요.",
            "생각보다 사람이 많지만 분위기가 좋다고 말해보세요.",
            "일본어로 자신의 취미와 자주 가는 여행지를 소개해보세요.",
            "추천받은 가게가 정말 좋았다고 감사 인사를 해보세요.",
            "상대방의 말을 잘 못 들었을 때 천천히 말해 달라고 부탁해보세요."
    };

    private StudyRepository repo;
    private ApiKeyStore keyStore;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int promptIndex = 0;
    private RubyTextView promptView;
    private EditText answerInput;
    private TextView feedback;
    private TextView timer;
    private int pendingSeconds = 0;
    private long lastInteraction = 0;
    private boolean resumed = false;

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
        keyStore = new ApiKeyStore(this);
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

        root.addView(Ui.title(this, "GPT 일본어 첨삭"));
        timer = Ui.text(this, "", 18, Color.WHITE);
        root.addView(timer);

        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "다음을 자연스러운 일본어로 써보세요.", 14, Color.LTGRAY));
        promptView = Ui.rubyText(this, PROMPTS[promptIndex], 20, Color.WHITE);
        promptView.setTextPadding(0, 8, 0, 10);
        card.addView(promptView);

        answerInput = new EditText(this);
        answerInput.setHint("일본어로 입력");
        answerInput.setTextColor(Color.WHITE);
        answerInput.setHintTextColor(Color.GRAY);
        answerInput.setMinLines(3);
        answerInput.setGravity(android.view.Gravity.TOP);
        card.addView(answerInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 130)));
        answerInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { markInteraction(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        Button submit = Ui.button(this, "GPT에게 첨삭 받기");
        submit.setOnClickListener(v -> submit(submit));
        card.addView(submit);

        Button next = Ui.button(this, "다른 문제");
        next.setOnClickListener(v -> {
            markInteraction();
            promptIndex = (promptIndex + 1) % PROMPTS.length;
            promptView.setText(PROMPTS[promptIndex]);
            answerInput.setText("");
            feedback.setText("");
        });
        card.addView(next);
        root.addView(card);

        feedback = Ui.text(this, "", 16, Color.LTGRAY);
        feedback.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 24));
        root.addView(feedback);

        if (!keyStore.hasApiKey()) {
            feedback.setText("API Key가 없습니다. 메인 → API / 앱 설정에서 키를 추가하면 사용할 수 있습니다. 기본 문제풀이는 API 없이도 가능합니다.");
        }
    }

    private void submit(Button button) {
        markInteraction();
        String answer = answerInput.getText().toString().trim();
        if (answer.isEmpty()) {
            Toast.makeText(this, "일본어 답변을 먼저 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!keyStore.hasApiKey()) {
            feedback.setText("API Key가 없습니다. 설정에서 먼저 추가해 주세요.");
            return;
        }
        button.setEnabled(false);
        feedback.setText("첨삭 중…");
        String task = PROMPTS[promptIndex];
        executor.execute(() -> {
            try {
                String input = "사용자 수준 " + StudyRepository.levelLabel(repo.getLevel()) + "\n한국어 과제: " + task + "\n사용자 일본어 답변: " + answer;
                String result = new OpenAiClient(keyStore).ask(
                        "당신은 한국인 성인 학습자를 위한 일본어 튜터입니다. 여행/친구 대화에서 실제 일본인이 쓰는 자연스러운 표현을 우선하세요. " +
                                "답변은 한국어로 1) 의미가 통하는지 2) 문법/어휘 수정 3) 더 자연스러운 일본어 한 문장 4) 핵심 포인트 순서로 짧고 명확하게 주세요.",
                        input);
                runOnUiThread(() -> {
                    feedback.setText(result);
                    button.setEnabled(true);
                    repo.recordGptFeedback(task, answer, result);
                    markInteraction();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    feedback.setText("GPT 호출 실패: " + e.getMessage());
                    button.setEnabled(true);
                });
            }
        });
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN || ev.getActionMasked() == MotionEvent.ACTION_MOVE) markInteraction();
        return super.dispatchTouchEvent(ev);
    }

    private void markInteraction() { lastInteraction = SystemClock.elapsedRealtime(); }

    private void flushPending() {
        if (pendingSeconds > 0) {
            int n = pendingSeconds;
            pendingSeconds = 0;
            repo.addStudySeconds(n);
        }
    }

    private void updateTimer() {
        int current = repo.getTodaySeconds() + pendingSeconds;
        timer.setText("학습 " + StudyRepository.formatSeconds(current) + " / " + StudyRepository.formatSeconds(repo.getTargetSeconds()));
    }

    @Override protected void onResume() { super.onResume(); resumed = true; updateTimer(); }
    @Override protected void onPause() { flushPending(); resumed = false; super.onPause(); }
    @Override protected void onDestroy() {
        flushPending();
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        flushPending();
        if (repo.isPenaltyActive()) {
            Toast.makeText(this, "벌칙 중에는 학습을 계속하거나 PASS를 사용해야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}
