package se.bjurr.prnfb.presentation.dto;

import java.util.Locale;

public enum ON_OR_OFF {
    on,
    off;

    public static ON_OR_OFF fromObject(Object o) {
        String s = o != null ? String.valueOf(o).trim() : null;
        if (s == null || "".equals(s)) {
            return null;
        } else {
            return ON_OR_OFF.valueOf(s.toLowerCase(Locale.ROOT));
        }
    }
}
