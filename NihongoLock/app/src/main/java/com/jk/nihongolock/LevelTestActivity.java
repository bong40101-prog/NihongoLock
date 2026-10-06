package com.jk.nihongolock;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class LevelTestActivity extends Activity {
    private StudyRepository repo;
    private List<QuestionBank.Q> questions;
    private int index = 0;
    private int correct = 0;
    private LinearLayout root;
    private LinearLayout box;
    private TextView progress;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.darkSystemBars(this);
        repo = new StudyRepository(this);
        questions = QuestionBank.levelTest();
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
        root.addView(Ui.title(this, "일본어 레벨 테스트"));
        root.addView(Ui.text(this, "12문제로 시작 난이도를 정합니다. 모르면 찍어도 괜찮습니다.", 14, Color.LTGRAY));
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
        progress.setText((index + 1) + " / " + questions.size());
        box.removeAllViews();
        TextView prompt = Ui.text(this, q.prompt, 20, Color.WHITE);
        prompt.setPadding(0, 0, 0, Ui.dp(this, 8));
        box.addView(prompt);
        for (int i = 0; i < q.options.length; i++) {
            final int choice = i;
            Button b = Ui.button(this, q.options[i]);
            b.setOnClickListener(v -> {
                if (choice == q.answer) correct++;
                index++;
                showQuestion();
            });
            box.addView(b);
        }
    }

    private void finishTest() {
        int total = questions.size();
        int level;
        double rate = total == 0 ? 0 : (double) correct / total;
        if (rate >= 0.90) level = 6;
        else if (rate >= 0.75) level = 5;
        else if (rate >= 0.58) level = 4;
        else if (rate >= 0.42) level = 3;
        else if (rate >= 0.25) level = 2;
        else level = 1;
        repo.setLevel(level);

        box.removeAllViews();
        progress.setText("테스트 완료");
        TextView result = Ui.text(this,
                "정답 " + correct + " / " + total + "\n시작 레벨: Lv." + level +
                        "\n\n이후 학습 정답률이 높으면 자동으로 올라가고, 계속 어려우면 내려갑니다.",
                20, Color.WHITE);
        box.addView(result);
        Button done = Ui.button(this, "완료");
        done.setOnClickListener(v -> finish());
        box.addView(done);
    }
}
