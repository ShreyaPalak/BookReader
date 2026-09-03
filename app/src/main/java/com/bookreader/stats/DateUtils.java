package com.bookreader.stats;

import java.util.Calendar;

/** Small helper for day-boundary math — every stats query needs "start of day N days ago". */
public class DateUtils {

    /** Start of today (midnight, device-local time) in epoch millis. */
    public static long startOfToday() {
        return startOfDay(0);
    }

    /** Start of the day `daysAgo` days before today. daysAgo=0 is today, 1 is yesterday, etc. */
    public static long startOfDay(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** Same as startOfDay(daysAgo) but for the following midnight — use as an exclusive range end. */
    public static long endOfDay(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startOfDay(daysAgo));
        cal.add(Calendar.DAY_OF_YEAR, 1);
        return cal.getTimeInMillis();
    }
}
