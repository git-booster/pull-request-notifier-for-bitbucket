package se.bjurr.prnfb.settings;

import java.util.Locale;

public enum TRIGGER_IF_MERGE {
    ALWAYS,
    CONFLICTING,
    NOT_CONFLICTING;

    public static TRIGGER_IF_MERGE fromObject(Object o) {
        String s = o != null ? String.valueOf(o).trim() : null;
        if (s == null || "".equals(s)) {
            return null;
        } else {
            return TRIGGER_IF_MERGE.valueOf(s.toUpperCase(Locale.ROOT));
        }
    }
}
