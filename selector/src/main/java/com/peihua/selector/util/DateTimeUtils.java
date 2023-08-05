package com.peihua.selector.util;

import static android.icu.text.DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE;
import static android.icu.text.RelativeDateTimeFormatter.Style.LONG;

import android.icu.text.DisplayContext;
import android.icu.text.RelativeDateTimeFormatter;
import android.icu.text.RelativeDateTimeFormatter.AbsoluteUnit;
import android.icu.text.RelativeDateTimeFormatter.Direction;
import android.icu.util.ULocale;
import android.os.Build;
import android.text.format.DateUtils;

import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;

/**
 * Provide the utility methods to handle date time.
 */
public class DateTimeUtils {

    private static final String DATE_FORMAT_SKELETON_WITH_YEAR = "EMMMdy";
    private static final String DATE_FORMAT_SKELETON_WITHOUT_YEAR = "EMMMd";
    private static final String DATE_FORMAT_SKELETON_WITH_TIME = "MMMdyhmmss";
    private static final String LA_DATE_FORMAT = "yyyyMMdd";
    static final SimpleDateFormat format = new SimpleDateFormat(LA_DATE_FORMAT);

    /**
     * Formats a time according to the local conventions for PhotoGrid.
     * <p>
     * If the difference of the date between the time and now is zero, show
     * "Today".
     * If the difference is 1, show "Yesterday".
     * If the difference is less than 7, show the weekday. E.g. "Sunday".
     * Otherwise, show the weekday and the date. E.g. "Sat, Jun 5".
     * If they have different years, show the weekday, the date and the year.
     * E.g. "Sat, Jun 5, 2021"
     *
     * @param when the time to be formatted. The unit is in milliseconds
     *             since January 1, 1970 00:00:00.0 UTC.
     * @return the formatted string
     */
    public static String getDateHeaderString(long when) {
        if (when < 1000000000000L) {
            when *= 1000;
        }
        // Get the system time zone
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            ZoneId zoneId = ZoneId.systemDefault();
            final LocalDate nowDate = LocalDate.now(zoneId);
            return getDateHeaderString(when, nowDate);
        }
        //LA_DATE_FORMAT
        return SimpleDateFormat.getDateInstance(DateFormat.DAY_OF_WEEK_IN_MONTH_FIELD).format(when);
    }

    /**
     * Formats a time according to the local conventions for content description.
     * <p>
     * The format of the returned string is fixed to {@code DATE_FORMAT_SKELETON_WITH_TIME}.
     * E.g. "Feb 2, 2022, 2:22:22 PM"
     *
     * @param when the time to be formatted. The unit is in milliseconds
     *             since January 1, 1970 00:00:00.0 UTC.
     * @return the formatted string
     */
    public static String getDateTimeStringForContentDesc(long when) {
        return getDateTimeString(when, DATE_FORMAT_SKELETON_WITH_TIME, Locale.getDefault());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @VisibleForTesting
    static String getDateHeaderString(long when, LocalDate nowDate) {
        // Get the system time zone
        final ZoneId zoneId = ZoneId.systemDefault();
        final LocalDate whenDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(when),
                zoneId).toLocalDate();

        final long dayDiff = ChronoUnit.DAYS.between(whenDate, nowDate);
        if (dayDiff == 0) {
            return getTodayString();
        } else if (dayDiff == 1) {
            return getYesterdayString();
        } else if (dayDiff > 0 && dayDiff < 7) {
            return whenDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
        } else {
            final String skeleton;
            if (whenDate.getYear() == nowDate.getYear()) {
                skeleton = DATE_FORMAT_SKELETON_WITHOUT_YEAR;
            } else {
                skeleton = DATE_FORMAT_SKELETON_WITH_YEAR;
            }

            return getDateTimeString(when, skeleton, Locale.getDefault());
        }
    }

    @VisibleForTesting
    static String getDateTimeString(long when, String skeleton, Locale locale) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            final android.icu.text.DateFormat format = android.icu.text.DateFormat.getInstanceForSkeleton(skeleton, locale);
            format.setContext(DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE);
            return format.format(when);
        }
        return format.format(when);
    }

    /**
     * It is borrowed from {@link DateUtils} since it is no official API yet.
     *
     * @param oneMillis the first time. The unit is in milliseconds since
     *                  January 1, 1970 00:00:00.0 UTC.
     * @param twoMillis the second time. The unit is in milliseconds since
     *                  January 1, 1970 00:00:00.0 UTC.
     * @return True, the date is the same. Otherwise, return false.
     */
    public static boolean isSameDate(long oneMillis, long twoMillis) {

        // Get the system time zone
        return format.format(new Date(oneMillis)).equals(format.format(new Date(twoMillis)));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @VisibleForTesting
    static String getTodayString() {
        final RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance(
                ULocale.getDefault(), null, LONG, CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE);
        return fmt.format(Direction.THIS, AbsoluteUnit.DAY);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @VisibleForTesting
    static String getYesterdayString() {
        final RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance(
                ULocale.getDefault(), null, LONG, CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE);
        return fmt.format(Direction.LAST, AbsoluteUnit.DAY);
    }
}
