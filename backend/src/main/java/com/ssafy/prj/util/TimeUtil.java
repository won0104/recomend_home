package com.ssafy.prj.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private TimeUtil() {
    }

    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
