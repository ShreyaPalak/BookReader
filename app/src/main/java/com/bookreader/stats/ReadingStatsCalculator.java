package com.bookreader.stats;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Streak/pages logic kept separate from the DAO calls that feed it, so this
 * class is plain testable Java with no Android/Room dependencies.
 */
public class ReadingStatsCalculator {

    public static class Stats {
        public int pagesToday;
        public int pagesYesterday;
        public int currentStreakDays;
    }

    /**
     * @param distinctDaysDescending output of SessionDao.getDistinctReadingDays() —
     *                                "YYYY-MM-DD" strings, most recent first
     */
    public static int computeStreak(List<String> distinctDaysDescending) {
        if (distinctDaysDescending == null || distinctDaysDescending.isEmpty()) {
            return 0;
        }

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = fmt.format(DateUtils.startOfToday());
        String mostRecentDay = distinctDaysDescending.get(0);

        // Streak is only "alive" if the most recent reading day was today or
        // yesterday. If the last session was 2+ days ago, the streak is broken —
        // return 0 rather than counting a stale run from before the gap.
        String yesterday = fmt.format(DateUtils.startOfDay(1));
        if (!mostRecentDay.equals(today) && !mostRecentDay.equals(yesterday)) {
            return 0;
        }

        int streak = 1;
        for (int i = 0; i < distinctDaysDescending.size() - 1; i++) {
            String current = distinctDaysDescending.get(i);
            String previous = distinctDaysDescending.get(i + 1);
            if (isExactlyOneDayBefore(previous, current, fmt)) {
                streak++;
            } else {
                break; // gap found — streak stops counting further back
            }
        }
        return streak;
    }

    private static boolean isExactlyOneDayBefore(String earlierDayStr, String laterDayStr, SimpleDateFormat fmt) {
        try {
            long earlier = fmt.parse(earlierDayStr).getTime();
            long later = fmt.parse(laterDayStr).getTime();
            long diffDays = Math.round((later - earlier) / 86400000.0);
            return diffDays == 1;
        } catch (Exception e) {
            return false; // malformed date string — treat as a gap rather than crash
        }
    }
}
