package com.jk.nihongolock;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends Activity {
    private ApiKeyStore keyStore;
    private EditText keyInput;
    private EditText modelInput;
    private TextView savedState;
    private TextView testState;
    private EditText updateRepoInput;
    private TextView updateState;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.darkSystemBars(this);
        keyStore = new ApiKeyStore(this);
        build();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(16,17,20));
        LinearLayout root = Ui.column(this);
        scroll.addView(root);
        setContentView(scroll);

        root.addView(Ui.title(this, "API / 앱 설정"));

        LinearLayout apiCard = Ui.card(this);
        apiCard.addView(Ui.text(this, "OpenAI API", 20, Color.WHITE));
        savedState = Ui.text(this, keyStore.maskedKey(), 14, Color.LTGRAY);
        savedState.setPadding(0, Ui.dp(this,6), 0, Ui.dp(this,8));
        apiCard.addView(savedState);

        keyInput = new EditText(this);
        keyInput.setHint("API Key 입력 (sk-...)");
        keyInput.setTextColor(Color.WHITE);
        keyInput.setHintTextColor(Color.GRAY);
        keyInput.setSingleLine(true);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        apiCard.addView(keyInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        modelInput = new EditText(this);
        modelInput.setHint("모델명");
        modelInput.setText(keyStore.getModel());
        modelInput.setTextColor(Color.WHITE);
        modelInput.setHintTextColor(Color.GRAY);
        modelInput.setSingleLine(true);
        apiCard.addView(modelInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button save = Ui.button(this, "API 설정 저장");
        save.setOnClickListener(v -> saveApi());
        apiCard.addView(save);

        Button test = Ui.button(this, "연결 테스트");
        test.setOnClickListener(v -> testApi(test));
        apiCard.addView(test);

        Button delete = Ui.button(this, "저장된 API Key 삭제");
        delete.setOnClickListener(v -> {
            keyStore.deleteApiKey();
            keyInput.setText("");
            savedState.setText("저장된 키 없음");
            Toast.makeText(this, "API Key를 삭제했습니다.", Toast.LENGTH_SHORT).show();
        });
        apiCard.addView(delete);

        testState = Ui.text(this, "", 14, Color.LTGRAY);
        testState.setPadding(0, Ui.dp(this,8), 0, 0);
        apiCard.addView(testState);
        root.addView(apiCard);

        LinearLayout updateCard = Ui.card(this);
        updateCard.addView(Ui.text(this, "앱 업데이트", 20, Color.WHITE));
        updateCard.addView(Ui.text(this,
                "GitHub Releases를 업데이트 서버로 사용합니다. 처음 한 번만 owner/repo를 설정하면 이후에는 앱에서 업데이트를 확인하고 덮어설치할 수 있습니다.",
                14, Color.LTGRAY));

        updateRepoInput = new EditText(this);
        updateRepoInput.setHint("GitHub owner/repo 또는 저장소 URL");
        updateRepoInput.setText(UpdateManager.getRepo(this));
        updateRepoInput.setTextColor(Color.WHITE);
        updateRepoInput.setHintTextColor(Color.GRAY);
        updateRepoInput.setSingleLine(true);
        updateCard.addView(updateRepoInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button saveRepo = Ui.button(this, "업데이트 서버 저장");
        saveRepo.setOnClickListener(v -> {
            String raw = updateRepoInput.getText().toString();
            String normalized = UpdateManager.normalizeRepo(raw);
            if (!raw.trim().isEmpty() && normalized.isEmpty()) {
                Toast.makeText(this, "owner/repo 형식으로 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            UpdateManager.setRepo(this, normalized);
            updateRepoInput.setText(normalized);
            Toast.makeText(this, normalized.isEmpty() ? "업데이트 서버 설정을 지웠습니다." : "업데이트 서버를 저장했습니다.", Toast.LENGTH_SHORT).show();
        });
        updateCard.addView(saveRepo);

        Button checkUpdate = Ui.button(this, "지금 업데이트 확인");
        checkUpdate.setOnClickListener(v -> {
            String raw = updateRepoInput.getText().toString();
            if (!raw.trim().isEmpty()) {
                String normalized = UpdateManager.normalizeRepo(raw);
                if (normalized.isEmpty()) {
                    updateState.setText("owner/repo 형식이 올바르지 않습니다.");
                    return;
                }
                UpdateManager.setRepo(this, normalized);
                updateRepoInput.setText(normalized);
            }
            checkUpdate.setEnabled(false);
            updateState.setText("최신 버전 확인 중…");
            UpdateManager.checkAsync(this, (info, error) -> runOnUiThread(() -> {
                checkUpdate.setEnabled(true);
                if (error != null) {
                    updateState.setText("확인 실패: " + error);
                } else if (info == null) {
                    updateState.setText("✓ 최신 버전입니다. 현재 v" + UpdateManager.currentVersionName(this));
                } else {
                    updateState.setText("새 버전 v" + info.versionName + " 사용 가능");
                    UpdateManager.showUpdateDialog(this, info);
                }
            }));
        });
        updateCard.addView(checkUpdate);

        updateState = Ui.text(this, "현재 앱 v" + UpdateManager.currentVersionName(this), 14, Color.LTGRAY);
        updateState.setPadding(0, Ui.dp(this,8), 0, 0);
        updateCard.addView(updateState);
        root.addView(updateCard);

        LinearLayout rule = Ui.card(this);
        rule.addView(Ui.text(this, "고정 학습 규칙", 20, Color.WHITE));
        rule.addView(Ui.text(this,
                "• 매일 20분\n• 미달 다음날 17:00~22:00 벌칙\n• 벌칙일 목표 35분\n• 35분 완료 시 즉시 해제\n• 22:00 자동 해제\n• 10일 연속 성공 = PASS 1장\n• PASS 최대 2장\n• 에이닷 전화/통화/긴급기능 예외\n• 앱 삭제 시 전체 종료",
                15, Color.LTGRAY));
        root.addView(rule);

        root.addView(Ui.text(this,
                "API Key는 Android Keystore의 AES-GCM 키로 암호화되어 이 기기에 저장됩니다. 앱 삭제 시 앱 데이터와 키도 함께 사라집니다. GPT가 없어도 기본 학습/벌칙 기능은 정상 동작합니다.",
                13, Color.GRAY));
    }

    private void saveApi() {
        try {
            String key = keyInput.getText().toString().trim();
            if (!key.isEmpty()) keyStore.saveApiKey(key);
            keyStore.setModel(modelInput.getText().toString());
            keyInput.setText("");
            savedState.setText(keyStore.maskedKey());
            Toast.makeText(this, "저장했습니다.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void testApi(Button button) {
        try {
            String typed = keyInput.getText().toString().trim();
            if (!typed.isEmpty()) keyStore.saveApiKey(typed);
            keyStore.setModel(modelInput.getText().toString());
            keyInput.setText("");
            savedState.setText(keyStore.maskedKey());
        } catch (Exception e) {
            testState.setText("저장 실패: " + e.getMessage());
            return;
        }
        if (!keyStore.hasApiKey()) {
            testState.setText("API Key를 먼저 입력해 주세요.");
            return;
        }
        button.setEnabled(false);
        testState.setText("연결 확인 중…");
        executor.execute(() -> {
            try {
                String result = new OpenAiClient(keyStore).ask(
                        "You are a connection test. Answer briefly in Korean.",
                        "'연결 성공'이라는 뜻이 드러나게 한 문장으로 답해줘.");
                runOnUiThread(() -> {
                    testState.setText("✓ " + result);
                    button.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    testState.setText("연결 실패: " + e.getMessage());
                    button.setEnabled(true);
                });
            }
        });
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
