package com.codicesoftware.plugins.hudson.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Adapted from the tfs plugin.
 */
public class DateUtil {

    public static final String DEFAULT_SORTABLE_FORMAT = "yyyy'-'MM'-'dd'T'HH':'mm':'ss";

    public static final String ISO_DATE_TIME_OFFSET_CSHARP_FORMAT = "yyyy'-'MM'-'dd'T'HH':'mm':'sszzz";

    public static final DateTimeFormatter DATETIME_UNIVERSAL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.of("UTC"));

    public static final DateTimeFormatter DATETIME_LOCAL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private DateUtil() {
    }

    public static final ThreadLocal<SimpleDateFormat> PLASTICSCM_DATETIME_FORMATTER = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DEFAULT_SORTABLE_FORMAT);
            dateFormat.setTimeZone(new SimpleTimeZone(0,"GMT"));
            return dateFormat;
        }
    };

    public static Date parseDate(String dateString) throws ParseException {
        return parseDate(dateString, Locale.getDefault(), TimeZone.getDefault());
    }

    public static Date parseDate(String dateString, Locale locale, TimeZone timezone) throws ParseException {
        Date date = tryParseDefaultFormat(dateString);
        
        if (date != null)
            return date;

        date = tryParseUnknownFormat(dateString);

        if (date != null)
            return date;

        // The old fashioned way did not work. Let's try it using a more
        // complex alternative.
        DateFormat[] formats = createDateFormatsForLocaleAndTimeZone(locale, timezone);
        return parseWithFormats(dateString, formats);
    }

    static Date tryParseDefaultFormat(String dateString)
    {
        try
        {
            return PLASTICSCM_DATETIME_FORMATTER.get().parse(dateString);
        }
        catch (ParseException e)
        {
            return null;
        }
    }
    
    static Date tryParseUnknownFormat(String dateString) {
        dateString = dateString.replaceAll("(p|P)\\.(m|M)\\.", "PM").replaceAll("(a|A)\\.(m|M)\\.", "AM");
        try {
            // Use the deprecated Date.parse method as this is very good at detecting
            // dates commonly output by the US and UK standard locales of dotnet that
            // are output by the Microsoft command line client.
            return new Date(Date.parse(dateString));
        } catch (IllegalArgumentException e) {
            // ignore - parse failed.
            return null;
        }
    }

    static Date parseWithFormats(String input, DateFormat[] formats) throws ParseException {
        ParseException parseException = null;
        for (int i = 0; i < formats.length; i++) {
            try {
                return formats[i].parse(input);
            } catch (ParseException ex) {
                parseException = ex;
            }
        }
        if (parseException == null) {
            throw new IllegalStateException("No dateformats found that can be used for parsing '" + input + "'");
        }
        throw parseException;
    }

    /**
     * Build an array of DateFormats that are commonly used for this locale
     * and timezone.
     */
    static DateFormat[] createDateFormatsForLocaleAndTimeZone(Locale locale, TimeZone timeZone) {
        List<DateFormat> formats = new ArrayList<DateFormat>();

        addDateTimeFormatsToList(locale, timeZone, formats);
        addDateFormatsToList(locale, timeZone, formats);

        return formats.toArray(new DateFormat[formats.size()]);
    }

    static void addDateFormatsToList(Locale locale, TimeZone timeZone, List<DateFormat> formats) {
        for (int dateStyle = DateFormat.FULL; dateStyle <= DateFormat.SHORT; dateStyle++) {
            DateFormat df = DateFormat.getDateInstance(dateStyle, locale);
            df.setTimeZone(timeZone);
            formats.add(df);
        }
    }

    static void addDateTimeFormatsToList(Locale locale, TimeZone timeZone, List<DateFormat> formats) {
        for (int dateStyle = DateFormat.FULL; dateStyle <= DateFormat.SHORT; dateStyle++) {
            for (int timeStyle = DateFormat.FULL; timeStyle <= DateFormat.SHORT; timeStyle++) {
                DateFormat df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
                if (timeZone != null) {
                    df.setTimeZone(timeZone);
                }
                formats.add(df);
            }
        }
    }
}
