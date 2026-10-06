package com.jk.nihongolock;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** A manually started promotion test: 8/10 advances exactly one level. */
public class LevelTestActivity extends Activity {
    private StudyRepository repo;
    private List<QuestionBank.Q> questions;
    private int index = 0;
    private int correct = 0;
    private int startingLevel;
    private LinearLayout root;
    private LinearLayout box;
    private TextView progress;
    private JapaneseSpeech speech;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.darkSystemBars(this);
        repo = new StudyRepository(this);
        startingLevel = repo.getLevel();
        speech = new JapaneseSpeech(this);
        questions = QuestionBank.levelTest(startingLevel);
        build();
        showQuestion();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(16,17,20));
        root = Ui.column(this);
        scroll.addView(root);
        setContentView(scroll);
        root.addView(Ui.title(this, "수동 레벨업 테스트"));
        root.addView(Ui.text(this,
                "현재 " + StudyRepository.levelLabel(startingLevel)
                        + " · 총 10문제 · 8개 이상 맞히면 다음 레벨로 올라갑니다.",
                14, Color.LTGRAY));
        root.addView(Ui.text(this,
                "이 테스트는 자동 등락과 별개로, 버튼을 눌렀을 때만 진행됩니다.",
                14, Color.rgb(160, 190, 255)));
        progress = Ui.text(this, "", 15, Color.LTGRAY);
        progress.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 8));
        root.addView(progress);
        box = Ui.card(this);
        root.addView(box);
    }

    private void showQuestion() {
        if (index >= questions.size()) {
            finishTest();
            return;
        }
        QuestionBank.Q q = QuestionBank.shuffled(questions.get(index));
        repo.markQuestionSeen(q.id);
        progress.setText((index + 1) + " / 10 · " + QuestionBank.kindLabel(q.kind));
        box.removeAllViews();
        RubyTextView prompt = Ui.rubyText(this, q.prompt, 20, Color.WHITE);
        prompt.setTextPadding(0, 0, 0, 8);
        box.addView(prompt);
        Button listen = Ui.button(this, "🔊 일본어 듣기");
        listen.setOnClickListener(v -> speech.speak(q.audioText));
        box.addView(listen);
        for (int i = 0; i < q.options.length; i++) {
            final int choice = i;
            RubyTextView b = Ui.rubyButton(this, q.options[i]);
            b.setOnClickListener(v -> {
                repo.recordLevelTestAnswer(q, choice);
                if (choice == q.answer) correct++;
                index++;
                showQuestion();
            });
            box.addView(b);
        }
    }

    private void finishTest() {
        int total = 10;
        boolean passed = correct >= 8;
        boolean promoted = repo.applyPromotionTest(correct, total);
        int afterLevel = repo.getLevel();

        box.removeAllViews();
        progress.setText("테스트 완료");
        String message;
        if (promoted) {
            message = "통과했습니다!\n" + StudyRepository.levelLabel(startingLevel)
                    + " → " + StudyRepository.levelLabel(afterLevel);
        } else if (startingLevel >= 6 && passed) {
            message = "정답 " + correct + " / 10\n이미 최고 레벨입니다.";
        } else {
            message = (passed ? "정답 8개 이상이지만 현재 레벨을 유지합니다." : "아직 레벨업 기준에 도달하지 못했습니다.")
                    + "\n정답 " + correct + " / 10\n현재 레벨: " + StudyRepository.levelLabel(afterLevel);
        }
        TextView result = Ui.text(this, message
                + "\n\n자동 레벨 등락은 학습 정답률에 따라 기존 방식으로 계속 유지됩니다.",
                20, Color.WHITE);
        box.addView(result);
        Button done = Ui.button(this, "완료");
        done.setOnClickListener(v -> finish());
        box.addView(done);
    }

    @Override protected void onDestroy() {
        if (speech != null) speech.shutdown();
        super.onDestroy();
    }
}
