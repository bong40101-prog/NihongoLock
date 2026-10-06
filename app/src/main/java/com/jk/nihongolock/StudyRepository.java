package com.jk.nihongolock;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class StudyRepository {
    public static final int BASIC_SECONDS = 20 * 60;
    public static final int PENALTY_SECONDS = 35 * 60;
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

    private final SharedPreferences p;

    public StudyRepository(Context context) {
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

    public synchronized void recordAnswer(boolean correct) {
        SharedPreferences.Editor e = p.edit().putInt(K_ANSWERED, p.getInt(K_ANSWERED,0) + 1);
        if (correct) e.putInt(K_CORRECT, p.getInt(K_CORRECT,0) + 1);
        e.apply();
        adaptLevel();
    }

    private void adaptLevel() {
        int answered = p.getInt(K_ANSWERED, 0);
        if (answered < 20 || answered % 10 != 0) return;
        int correct = p.getInt(K_CORRECT, 0);
        double rate = answered == 0 ? 0 : (double) correct / answered;
        int level = getLevel();
        if (rate >= 0.85 && level < 6) level++;
        else if (rate < 0.60 && level > 1) level--;
        p.edit().putInt(K_LEVEL, level).putInt(K_ANSWERED, 0).putInt(K_CORRECT, 0).apply();
    }

    public static String formatSeconds(int seconds) {
        int m = Math.max(0, seconds) / 60;
        int s = Math.max(0, seconds) % 60;
        return String.format(java.util.Locale.KOREA, "%02d:%02d", m, s);
    }
}
