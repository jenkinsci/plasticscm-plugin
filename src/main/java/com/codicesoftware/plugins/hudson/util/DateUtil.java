package com.codicesoftware.plugins.hudson.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    public static final String DEFAULT_SORTABLE_FORMAT = "yyyy'-'MM'-'dd'T'HH':'mm':'ss";

    public static final String ISO_DATE_TIME_OFFSET_CSHARP_FORMAT = "yyyy'-'MM'-'dd'T'HH':'mm':'sszzz";

    public static final DateTimeFormatter DATETIME_XML_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    public static final DateTimeFormatter DATETIME_UNIVERSAL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.of("UTC"));

    public static final DateTimeFormatter DATETIME_LOCAL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private DateUtil() { }
}
