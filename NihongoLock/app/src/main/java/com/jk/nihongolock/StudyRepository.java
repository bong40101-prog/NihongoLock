package com.jk.nihongolock;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

public class StudyRepository {
    public static final int BASIC_SECONDS = 20 * 60;
    public static final int PENALTY_SECONDS = 45 * 60;
    public static final int MAX_PASSES = 2;

    private static final String PREFS = "study_state_v1";
    private static final String K_DATE = "date";
    private static final String K_SECONDS = "seconds";
    private static final String K_STREAK = "streak";
    private static final String K_PASSES = "passes";
    private static final String K_CREDITED_DATE = "credited_date";
    private static final String K_PENALTY_DATE = "penalty_date";
    private static final String K_PENALTY_WAIVED_DATE = "penalty_waived_date";
    private static final String K_PENALTY_COMPLETE_DATE = "penalty_complete_date";
    private static final String K_PENDING_RESET = "pending_reset";
    private static final String K_FAILURE_SOURCE_DATE = "failure_source_date";
    private static final String K_LEVEL = "level";
    private static final String K_LEVEL_TEST_DONE = "level_test_done";
    private static final String K_CORRECT = "correct";
    private static final String K_ANSWERED = "answered";
    private static final String K_TOTAL_CORRECT = "total_correct";
    private static final String K_TOTAL_ANSWERED = "total_answered";
    private static final String K_ANSWER_LOG = "answer_log";
    private static final String K_PRACTICE_LOG = "practice_log";
    private static final String K_RECENT_QUESTION_IDS = "recent_question_ids";
    private static final int MAX_ANSWER_LOGS = 1000;
    private static final int MAX_PRACTICE_LOGS = 200;
    private static final int MAX_RECENT_QUESTIONS = 40;

    private final Context context;
    private final SharedPreferences p;

    public StudyRepository(Context context) {
        this.context = context.getApplicationContext();
        p = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void reconcile() {
        LocalDate today = LocalDate.now();
        String storedRaw = p.getString(K_DATE, null);
        if (storedRaw == null) {
            p.edit().putString(K_DATE, today.toString()).putInt(K_SECONDS, 0).apply();
            storedRaw = today.toString();
        }

        LocalDate stored;
        try {
            stored = LocalDate.parse(storedRaw);
        } catch (Exception e) {
            stored = today;
            p.edit().putString(K_DATE, today.toString()).putInt(K_SECONDS, 0).apply();
        }

        if (!stored.equals(today)) {
            int storedSeconds = p.getInt(K_SECONDS, 0);

            // If yesterday was already serving an older punishment and its unresolved
            // failure never got finalized, finalize it now before evaluating yesterday.
            if (p.getBoolean(K_PENDING_RESET, false)) {
                p.edit().putInt(K_STREAK, 0).putBoolean(K_PENDING_RESET, false).apply();
                if (storedSeconds >= BASIC_SECONDS) {
                    creditDateIfNeeded(stored.toString());
                }
            } else if (storedSeconds >= BASIC_SECONDS) {
                creditDateIfNeeded(stored.toString());
            }

            long gap = ChronoUnit.DAYS.between(stored, today);
            SharedPreferences.Editor e = p.edit()
                    .putString(K_DATE, today.toString())
                    .putInt(K_SECONDS, 0);

            if (gap == 1 && storedSeconds < BASIC_SECONDS) {
                e.putString(K_PENALTY_DATE, today.toString())
                        .putString(K_FAILURE_SOURCE_DATE, stored.toString())
                        .putBoolean(K_PENDING_RESET, true)
                        .remove(K_PENALTY_WAIVED_DATE)
                        .remove(K_PENALTY_COMPLETE_DATE);
            } else if (gap > 1) {
                // Multiple missed dates definitely break the streak. Yesterday is also
                // missed, so today still gets a punishment.
                e.putInt(K_STREAK, 0)
                        .putString(K_PENALTY_DATE, today.toString())
                        .putString(K_FAILURE_SOURCE_DATE, today.minusDays(1).toString())
                        .putBoolean(K_PENDING_RESET, true)
                        .remove(K_PENALTY_WAIVED_DATE)
                        .remove(K_PENALTY_COMPLETE_DATE);
            } else {
                e.remove(K_PENALTY_DATE)
                        .remove(K_FAILURE_SOURCE_DATE)
                        .putBoolean(K_PENDING_RESET, false)
                        .remove(K_PENALTY_WAIVED_DATE)
                        .remove(K_PENALTY_COMPLETE_DATE);
            }
            e.apply();
        }

        String todayStr = today.toString();
        int seconds = p.getInt(K_SECONDS, 0);
        boolean penaltyToday = todayStr.equals(p.getString(K_PENALTY_DATE, ""));
        boolean waived = todayStr.equals(p.getString(K_PENALTY_WAIVED_DATE, ""));
        boolean complete = todayStr.equals(p.getString(K_PENALTY_COMPLETE_DATE, ""));

        if (penaltyToday && !waived && !complete && seconds >= PENALTY_SECONDS) {
            p.edit().putString(K_PENALTY_COMPLETE_DATE, todayStr).apply();
            resolvePendingFailureWithoutPass();
            complete = true;
        }

        if (penaltyToday && !waived && !complete && !LocalTime.now().isBefore(LocalTime.of(22, 0))) {
            resolvePendingFailureWithoutPass();
        }

        if (!p.getBoolean(K_PENDING_RESET, false) && seconds >= BASIC_SECONDS) {
            creditDateIfNeeded(todayStr);
        }
    }

    private synchronized void resolvePendingFailureWithoutPass() {
        if (p.getBoolean(K_PENDING_RESET, false)) {
            p.edit().putInt(K_STREAK, 0).putBoolean(K_PENDING_RESET, false).apply();
        }
        String today = LocalDate.now().toString();
        if (p.getInt(K_SECONDS, 0) >= BASIC_SECONDS) {
            creditDateIfNeeded(today);
        }
    }

    private synchronized void creditDateIfNeeded(String date) {
        String credited = p.getString(K_CREDITED_DATE, "");
        if (date.equals(credited)) return;
        int streak = p.getInt(K_STREAK, 0) + 1;
        int passes = p.getInt(K_PASSES, 0);
        if (streak % 10 == 0 && passes < MAX_PASSES) {
            passes++;
        }
        p.edit()
                .putInt(K_STREAK, streak)
                .putInt(K_PASSES, passes)
                .putString(K_CREDITED_DATE, date)
                .apply();
    }

    public synchronized void addStudySeconds(int seconds) {
        if (seconds <= 0) return;
        reconcile();
        int next = Math.max(0, p.getInt(K_SECONDS, 0) + seconds);
        p.edit().putInt(K_SECONDS, next).apply();
        reconcile();
        GitHubStudySync.schedule(context);
    }

    public synchronized int getTodaySeconds() {
        reconcile();
        return p.getInt(K_SECONDS, 0);
    }

    public synchronized int getStreak() {
        reconcile();
        return p.getInt(K_STREAK, 0);
    }

    public synchronized int getPasses() {
        reconcile();
        return p.getInt(K_PASSES, 0);
    }

    public synchronized boolean isPenaltyToday() {
        reconcile();
        return LocalDate.now().toString().equals(p.getString(K_PENALTY_DATE, ""))
                && !LocalDate.now().toString().equals(p.getString(K_PENALTY_WAIVED_DATE, ""));
    }

    public synchronized boolean isPenaltyComplete() {
        reconcile();
        return LocalDate.now().toString().equals(p.getString(K_PENALTY_COMPLETE_DATE, ""));
    }

    public synchronized boolean isPenaltyActive() {
        reconcile();
        LocalTime now = LocalTime.now();
        boolean inWindow = !now.isBefore(LocalTime.of(17, 0)) && now.isBefore(LocalTime.of(22, 0));
        return inWindow && isPenaltyToday() && !isPenaltyComplete();
    }

    public synchronized boolean isPenaltyPending() {
        reconcile();
        LocalTime now = LocalTime.now();
        return now.isBefore(LocalTime.of(17, 0)) && isPenaltyToday() && !isPenaltyComplete();
    }

    public synchronized boolean canUsePass() {
        reconcile();
        return getPassesRaw() > 0
                && LocalTime.now().isBefore(LocalTime.of(22, 0))
                && LocalDate.now().toString().equals(p.getString(K_PENALTY_DATE, ""))
                && !LocalDate.now().toString().equals(p.getString(K_PENALTY_WAIVED_DATE, ""))
                && !LocalDate.now().toString().equals(p.getString(K_PENALTY_COMPLETE_DATE, ""));
    }

    public synchronized boolean usePass() {
        reconcile();
        if (!canUsePass()) return false;
        int passes = getPassesRaw();
        String today = LocalDate.now().toString();
        p.edit()
                .putInt(K_PASSES, passes - 1)
                .putString(K_PENALTY_WAIVED_DATE, today)
                .putBoolean(K_PENDING_RESET, false)
                .apply();
        if (p.getInt(K_SECONDS, 0) >= BASIC_SECONDS) creditDateIfNeeded(today);
        GitHubStudySync.schedule(context);
        return true;
    }

    private int getPassesRaw() {
        return p.getInt(K_PASSES, 0);
    }

    public synchronized int getTargetSeconds() {
        reconcile();
        LocalTime now = LocalTime.now();
        if (isPenaltyToday() && !isPenaltyComplete() && now.isBefore(LocalTime.of(22, 0))) {
            return PENALTY_SECONDS;
        }
        return BASIC_SECONDS;
    }

    public int secondsUntilPenaltyEnd() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(17,0)) || !now.isBefore(LocalTime.of(22,0))) return 0;
        return (int) Duration.between(now, LocalTime.of(22,0)).getSeconds();
    }

    public synchronized void setLevel(int level) {
        p.edit().putInt(K_LEVEL, Math.max(1, Math.min(6, level))).putBoolean(K_LEVEL_TEST_DONE, true).apply();
    }

    public synchronized int getLevel() {
        return p.getInt(K_LEVEL, 1);
    }

    public synchronized boolean isLevelTestDone() {
        return p.getBoolean(K_LEVEL_TEST_DONE, false);
    }

    /** The user starts this test manually; eight of ten promotes one level. */
    public synchronized boolean applyPromotionTest(int correct, int total) {
        if (total != 10 || correct < 8 || getLevel() >= 6) return false;
        setLevel(getLevel() + 1);
        return true;
    }

    public synchronized Set<String> getRecentQuestionIds() {
        Set<String> ids = new HashSet<>();
        try {
            JSONArray array = new JSONArray(p.getString(K_RECENT_QUESTION_IDS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                String id = array.optString(i, "");
                if (!id.isEmpty()) ids.add(id);
            }
        } catch (Exception ignored) {}
        return ids;
    }

    /** Remembers the last questions shown, including across app restarts. */
    public synchronized void markQuestionSeen(String id) {
        if (id == null || id.trim().isEmpty()) return;
        JSONArray array;
        try {
            array = new JSONArray(p.getString(K_RECENT_QUESTION_IDS, "[]"));
        } catch (Exception ignored) {
            array = new JSONArray();
        }
        for (int i = array.length() - 1; i >= 0; i--) {
            if (id.equals(array.optString(i, ""))) array.remove(i);
        }
        array.put(id);
        while (array.length() > MAX_RECENT_QUESTIONS) array.remove(0);
        p.edit().putString(K_RECENT_QUESTION_IDS, array.toString()).apply();
    }

    public synchronized String exportJson() {
        reconcile();
        JSONObject root = new JSONObject();
        try {
            root.put("schemaVersion", 2);
            root.put("exportedAt", Instant.now().toString());
            root.put("date", LocalDate.now().toString());
            root.put("todayStudySeconds", p.getInt(K_SECONDS, 0));
            root.put("targetSeconds", getTargetSeconds());
            root.put("streakDays", p.getInt(K_STREAK, 0));
            root.put("passes", p.getInt(K_PASSES, 0));
            root.put("level", getLevel());
            root.put("levelLabel", levelLabel(getLevel()));
            root.put("totalAnswered", p.getInt(K_TOTAL_ANSWERED, 0));
            root.put("totalCorrect", p.getInt(K_TOTAL_CORRECT, 0));
            root.put("answerHistory", new JSONArray(p.getString(K_ANSWER_LOG, "[]")));
            root.put("practiceHistory", new JSONArray(p.getString(K_PRACTICE_LOG, "[]")));
        } catch (Exception e) {
            return "{\"schemaVersion\":2,\"error\":\"export_failed\"}";
        }
        try {
            return root.toString(2);
        } catch (Exception ignored) {
            return root.toString();
        }
    }

    public static String levelLabel(int level) {
        switch (Math.max(1, Math.min(6, level))) {
            case 1: return "Lv.1 입문";
            case 2: return "Lv.2 N5 기초";
            case 3: return "Lv.3 N4 기초";
            case 4: return "Lv.4 N3 기초";
            case 5: return "Lv.5 N3 심화·N2 준비";
            default: return "Lv.6 N2 심화·N1 준비";
        }
    }

    public synchronized void recordAnswer(boolean correct) {
        recordAnswerInternal(null, -1, correct, "study", true, null);
    }

    public synchronized void recordAnswer(QuestionBank.Q question, int chosen, String source) {
        if (question == null) {
            recordAnswerInternal(null, chosen, false, source, true, null);
            return;
        }
        recordAnswerInternal(question, chosen, chosen == question.answer, source, true, null);
    }

    public synchronized void recordLevelTestAnswer(QuestionBank.Q question, int chosen) {
        if (question == null) return;
        recordAnswerInternal(question, chosen, chosen == question.answer, "level_test", false, null);
    }

    public synchronized void recordPracticeAnswer(QuestionBank.Q question, String source,
                                                   String typedAnswer, boolean correct) {
        if (question == null) return;
        recordAnswerInternal(question, -1, correct, source, true, typedAnswer);
    }

    public synchronized void recordGptFeedback(String prompt, String typedAnswer, String feedback) {
        JSONArray logs;
        try {
            logs = new JSONArray(p.getString(K_PRACTICE_LOG, "[]"));
        } catch (Exception ignored) {
            logs = new JSONArray();
        }
        JSONObject row = new JSONObject();
        try {
            row.put("at", Instant.now().toString());
            row.put("source", "gpt_feedback");
            row.put("prompt", prompt == null ? "" : prompt);
            row.put("typed", typedAnswer == null ? "" : typedAnswer);
            row.put("feedback", feedback == null ? "" : feedback);
            logs.put(row);
            while (logs.length() > MAX_PRACTICE_LOGS) logs.remove(0);
            p.edit().putString(K_PRACTICE_LOG, logs.toString()).apply();
        } catch (Exception ignored) {}
        GitHubStudySync.schedule(context);
    }

    private void recordAnswerInternal(QuestionBank.Q question, int chosen, boolean correct,
                                      String source, boolean adaptLevel, String typedAnswer) {
        SharedPreferences.Editor e = p.edit()
                .putInt(K_ANSWERED, p.getInt(K_ANSWERED, 0) + 1)
                .putInt(K_TOTAL_ANSWERED, p.getInt(K_TOTAL_ANSWERED, 0) + 1);
        if (correct) {
            e.putInt(K_CORRECT, p.getInt(K_CORRECT, 0) + 1)
                    .putInt(K_TOTAL_CORRECT, p.getInt(K_TOTAL_CORRECT, 0) + 1);
        }
        appendAnswerLog(question, chosen, correct, source, typedAnswer);
        e.apply();
        if (adaptLevel) adaptLevel();
        GitHubStudySync.schedule(context);
    }

    private void appendAnswerLog(QuestionBank.Q question, int chosen, boolean correct, String source,
                                 String typedAnswer) {
        JSONArray logs;
        try {
            logs = new JSONArray(p.getString(K_ANSWER_LOG, "[]"));
        } catch (Exception ignored) {
            logs = new JSONArray();
        }
        JSONObject row = new JSONObject();
        try {
            row.put("at", Instant.now().toString());
            row.put("source", source == null ? "study" : source);
            row.put("correct", correct);
            row.put("level", getLevel());
            if (question != null) {
                row.put("questionId", question.id);
                row.put("kind", question.kind);
                row.put("question", question.prompt);
                row.put("audio", question.audioText);
                row.put("selected", chosen >= 0 && chosen < question.options.length ? question.options[chosen] : "");
                row.put("answer", question.answer >= 0 && question.answer < question.options.length
                        ? question.options[question.answer] : "");
                if (typedAnswer != null) row.put("typed", typedAnswer);
            }
            logs.put(row);
            while (logs.length() > MAX_ANSWER_LOGS) logs.remove(0);
            p.edit().putString(K_ANSWER_LOG, logs.toString()).apply();
        } catch (Exception ignored) {
            // A failed history row must never interrupt a learning session.
        }
    }

    private void adaptLevel() {
        int answered = p.getInt(K_ANSWERED, 0);
        if (answered < 20 || answered % 10 != 0) return;
        int correct = p.getInt(K_CORRECT, 0);
        double rate = answered == 0 ? 0 : (double) correct / answered;
        int level = getLevel();
        // Keep automatic movement conservative: promotion needs 18/20 correct,
        // while 13/20 or fewer moves the learner down for review.
        if (rate >= 0.90 && level < 6) level++;
        else if (rate < 0.65 && level > 1) level--;
        p.edit().putInt(K_LEVEL, level).putInt(K_ANSWERED, 0).putInt(K_CORRECT, 0).apply();
    }

    public static String formatSeconds(int seconds) {
        int m = Math.max(0, seconds) / 60;
        int s = Math.max(0, seconds) % 60;
        return String.format(java.util.Locale.KOREA, "%02d:%02d", m, s);
    }
}
