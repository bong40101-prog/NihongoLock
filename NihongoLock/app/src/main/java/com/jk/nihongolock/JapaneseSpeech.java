package com.jk.nihongolock;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts the Japanese expression from a question and reads it aloud. */
public final class JapaneseSpeech {
    private static final Pattern QUOTED = Pattern.compile("『([^』]+)』");

    private final Context context;
    private TextToSpeech tts;
    private boolean initialized;
    private boolean japaneseAvailable;

    public JapaneseSpeech(Context context) {
        this.context = context.getApplicationContext();
        tts = new TextToSpeech(this.context, status -> {
            initialized = status == TextToSpeech.SUCCESS;
            if (initialized) {
                int result = tts.setLanguage(Locale.JAPAN);
                japaneseAvailable = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
                tts.setSpeechRate(0.9f);
            }
        });
    }

    public void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (!initialized || !japaneseAvailable) {
            Toast.makeText(context, "일본어 음성 데이터를 사용할 수 없습니다. 휴대폰 TTS 설정을 확인해 주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        tts.stop();
        tts.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, null, "nihongolock-japanese");
    }

    public void shutdown() {
        tts.stop();
        tts.shutdown();
    }

    public static String extract(String prompt, String[] options, int answer) {
        if (prompt != null) {
            Matcher matcher = QUOTED.matcher(prompt);
            if (matcher.find() && containsJapanese(matcher.group(1))) return matcher.group(1);
        }
        if (options != null && answer >= 0 && answer < options.length && containsJapanese(options[answer])) {
            return options[answer];
        }
        if (options != null) {
            for (String option : options) if (containsJapanese(option)) return option;
        }
        return prompt == null ? "" : prompt;
    }

    private static boolean containsJapanese(String text) {
        return text != null && text.matches(".*[ぁ-んァ-ヶ一-龯].*");
    }
}
