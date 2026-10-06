package com.jk.nihongolock;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Offline Japanese speaking practice using Android speech recognition. */
public class SpeakingActivity extends Activity implements RecognitionListener {
    private static final int REQUEST_RECORD_AUDIO = 321;
    public static final String EXTRA_AUTO_START = "auto_start";
    public static final String EXTRA_FROM_STUDY = "from_study";

    private StudyRepository repo;
    private JapaneseSpeech speech;
    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private List<QuestionBank.Q> questions;
    private QuestionBank.Q current;
    private int index;
    private boolean listening;
    private boolean answered;
    private TextView progress;
    private TextView status;
    private TextView transcript;
    private RubyTextView target;
    private TextView meaning;
    private Button startButton;
    private Button nextButton;
    private TextView timer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ToneGenerator toneGenerator;
    private int pendingSeconds;
    private long lastInteraction;
    private boolean resumed;
    private boolean autoStart;
    private boolean fromStudy;
    private boolean autoStartRequested;
    private boolean endTonePlayed;
    private boolean finishScheduled;
    private int recognitionAttempt;

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
        autoStart = getIntent().getBooleanExtra(EXTRA_AUTO_START, false);
        fromStudy = getIntent().getBooleanExtra(EXTRA_FROM_STUDY, false);
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 82);
        } catch (RuntimeException ignored) {
            toneGenerator = null;
        }
        questions = QuestionBank.speakingForLevel(repo.getLevel(), repo.getRecentQuestionIds());
        if (questions.isEmpty()) questions = QuestionBank.speakingForLevel(repo.getLevel(), new HashSet<>());
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.JAPAN.toLanguageTag());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        // Give the learner time to pause between words without cutting the sentence off.
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 10000L);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 6000L);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
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

        root.addView(Ui.title(this, "말하기 연습"));
        timer = Ui.text(this, "", 18, Color.WHITE);
        root.addView(timer);
        root.addView(Ui.text(this,
                autoStart
                        ? "이번 말하기 문제는 20분 학습에 자동으로 포함되었습니다. 시작음이 나면 문장 전체를 말해 주세요."
                        : "일본어 음성을 듣고 따라 말해 보세요. 이 시간은 메인 20분 학습에 합산됩니다. 휴대폰의 일본어 음성 인식 기능을 사용합니다.",
                14, Color.LTGRAY));

        LinearLayout card = Ui.card(this);
        progress = Ui.text(this, "", 14, Color.rgb(151,182,255));
        card.addView(progress);
        target = Ui.rubyText(this, "", 25, Color.WHITE);
        target.setTextPadding(0, 12, 0, 4);
        card.addView(target);
        meaning = Ui.text(this, "", 16, Color.LTGRAY);
        meaning.setPadding(0, 0, 0, Ui.dp(this, 8));
        card.addView(meaning);
        Button listen = Ui.button(this, "🔊 정답 문장 듣기");
        listen.setOnClickListener(v -> {
            markInteraction();
            if (current != null) speech.speak(current.writingAnswer);
        });
        card.addView(listen);
        startButton = Ui.button(this, "🎙 말하기 시작");
        startButton.setOnClickListener(v -> startListening());
        card.addView(startButton);
        Button stop = Ui.button(this, "말하기 중지");
        stop.setOnClickListener(v -> stopListening());
        card.addView(stop);
        transcript = Ui.text(this, "인식 결과가 여기에 표시됩니다.", 16, Color.LTGRAY);
        transcript.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(transcript);
        status = Ui.text(this, "", 16, Color.LTGRAY);
        status.setPadding(0, Ui.dp(this, 8), 0, 0);
        card.addView(status);
        nextButton = Ui.button(this, "다음 말하기 문제");
        nextButton.setOnClickListener(v -> {
            markInteraction();
            stopListening();
            index = (index + 1) % questions.size();
            showQuestion();
        });
        nextButton.setVisibility(autoStart ? View.GONE : View.VISIBLE);
        card.addView(nextButton);
        root.addView(card);
    }

    private void showQuestion() {
        current = questions.get(index);
        repo.markQuestionSeen(current.id);
        answered = false;
        recognitionAttempt = 0;
        finishScheduled = false;
        progress.setText((index + 1) + " / " + questions.size() + " · 문장 말하기");
        target.setText(current.writingAnswer);
        meaning.setText("뜻: " + current.koreanMeaning);
        transcript.setText("인식 결과가 여기에 표시됩니다.");
        status.setText(autoStart
                ? "잠시 후 시작음이 납니다. 문장 전체를 천천히 말해 주세요."
                : "정답 문장을 먼저 듣고 말해 보세요.");
        status.setTextColor(Color.LTGRAY);
        startButton.setEnabled(true);
        if (autoStart) startButton.setText("🎙 자동 시작 대기 중…");
    }

    private void startListening() {
        if (listening) return;
        markInteraction();
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("이 휴대폰에서 음성 인식을 사용할 수 없습니다.");
            finishAfterAutoRound();
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(this);
        }
        endTonePlayed = false;
        listening = true;
        startButton.setText("🎙 듣는 중…");
        startButton.setEnabled(!autoStart);
        status.setText("시작음 후 일본어 문장 전체를 말해 주세요. 잠깐 쉬어도 계속 듣습니다.");
        playStartTone();
        try {
            recognizer.startListening(recognizerIntent);
        } catch (RuntimeException e) {
            listening = false;
            playEndToneOnce();
            status.setText("음성 인식을 시작하지 못했습니다. 다시 시도해 주세요.");
            startButton.setEnabled(true);
            finishAfterAutoRound();
        }
    }

    private void stopListening() {
        if (recognizer != null && listening) recognizer.stopListening();
        if (listening) playEndToneOnce();
        listening = false;
        if (startButton != null) {
            startButton.setText("🎙 말하기 시작");
            startButton.setEnabled(true);
        }
    }

    private void handleResult(String heard) {
        listening = false;
        startButton.setText("🎙 말하기 시작");
        startButton.setEnabled(true);
        playEndToneOnce();
        transcript.setText("인식 결과: " + heard);
        boolean correct = matches(current.writingAnswer, heard);
        if (!correct && fromStudy && recognitionAttempt < 1) {
            recognitionAttempt++;
            status.setText("문장이 중간에 끝났거나 잘 안 들렸어요. 잠시 후 한 번 더 말해 주세요.");
            status.setTextColor(Color.rgb(255, 180, 80));
            handler.postDelayed(() -> {
                if (resumed && !isFinishing()) startListening();
            }, 800L);
            return;
        }
        if (!answered) {
            answered = true;
            repo.recordPracticeAnswer(current, "speaking", heard, correct);
        }
        status.setText(correct
                ? "✓ 잘했어요. 목표 문장과 가깝습니다."
                : "△ 다시 들어 보세요. 목표: " + current.writingAnswer);
        status.setTextColor(correct ? Color.rgb(101,209,138) : Color.rgb(255,180,80));
        finishAfterAutoRound();
    }

    private void finishAfterAutoRound() {
        if (!fromStudy || finishScheduled) return;
        finishScheduled = true;
        handler.postDelayed(this::finish, 1800L);
    }

    private void playStartTone() {
        if (toneGenerator == null) return;
        try {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
        } catch (RuntimeException ignored) {}
    }

    private void playEndToneOnce() {
        if (endTonePlayed || toneGenerator == null) return;
        endTonePlayed = true;
        try {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 180);
        } catch (RuntimeException ignored) {}
    }

    private boolean matches(String targetText, String heard) {
        String targetNormalized = normalize(targetText);
        String readingNormalized = normalize(Furigana.toReading(targetText));
        String heardNormalized = normalize(heard);
        if (heardNormalized.isEmpty()) return false;
        return heardNormalized.equals(targetNormalized)
                || heardNormalized.equals(readingNormalized)
                || (heardNormalized.length() >= minimumAcceptedLength(targetNormalized)
                    && targetNormalized.contains(heardNormalized))
                || (heardNormalized.length() >= minimumAcceptedLength(readingNormalized)
                    && readingNormalized.contains(heardNormalized))
                || heardNormalized.contains(targetNormalized);
    }

    private int minimumAcceptedLength(String expected) {
        if (expected.length() <= 5) return expected.length();
        return Math.max(5, (int) Math.ceil(expected.length() * 0.65));
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

    @Override public void onReadyForSpeech(Bundle params) { status.setText("듣고 있습니다…"); }
    @Override public void onBeginningOfSpeech() { status.setText("말하는 중…"); }
    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() {
        playEndToneOnce();
        status.setText("확인 중…");
    }
    @Override public void onPartialResults(Bundle partialResults) {
        ArrayList<String> values = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (values != null && !values.isEmpty()) transcript.setText("인식 중: " + values.get(0));
    }
    @Override public void onEvent(int eventType, Bundle params) {}
    @Override public void onError(int error) {
        listening = false;
        playEndToneOnce();
        if (startButton != null) startButton.setText("🎙 말하기 시작");
        if (startButton != null) startButton.setEnabled(true);
        if (fromStudy && recognitionAttempt < 1) {
            recognitionAttempt++;
            status.setText("음성이 짧게 끝났어요. 잠시 후 한 번 더 말해 주세요.");
            status.setTextColor(Color.rgb(255, 180, 80));
            handler.postDelayed(() -> {
                if (resumed && !isFinishing()) startListening();
            }, 800L);
            return;
        }
        status.setText("음성 인식이 끝나지 않았습니다. 다음 문제에서 다시 시도합니다. (" + error + ")");
        finishAfterAutoRound();
    }
    @Override public void onResults(Bundle results) {
        ArrayList<String> values = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        handleResult(values == null || values.isEmpty() ? "" : values.get(0));
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) startListening();
        else {
            status.setText("말하기 연습에는 마이크 권한이 필요합니다.");
            if (startButton != null) startButton.setEnabled(true);
            finishAfterAutoRound();
        }
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN || ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            markInteraction();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        updateTimer();
        if (autoStart && !autoStartRequested) {
            autoStartRequested = true;
            handler.postDelayed(() -> {
                if (resumed && !isFinishing()) startListening();
            }, 700L);
        }
    }
    @Override protected void onPause() { flushPending(); resumed = false; stopListening(); super.onPause(); }
    @Override protected void onDestroy() {
        flushPending();
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) recognizer.destroy();
        if (toneGenerator != null) toneGenerator.release();
        if (speech != null) speech.shutdown();
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
