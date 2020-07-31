package com.app.player.util;

import java.util.Locale;

public class DurationUtil {
    public static String durationToStrMs(int duration, boolean includeMs) {
        int ms = duration % 1000;
        int s = duration / 1000;
        int m = 0;
        if (s >= 60) {
            m = s / 60;
            s = s % 60;
        }
        if (includeMs) {
            return String.format(Locale.CHINA, "%02d:%02d.%03d", m, s, ms);
        }
        return String.format(Locale.CHINA, "%02d:%02d", m, s);
    }
}
