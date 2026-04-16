package se.bjurr.prnfb.settings;

import java.util.Locale;

public enum USER_LEVEL {
    ADMIN,
    EVERYONE,
    SYSTEM_ADMIN;

    public static USER_LEVEL fromObject(Object o) {
        String s = o != null ? String.valueOf(o).trim().toUpperCase(Locale.ROOT) : null;
        if (s == null || "".equals(s)) {
            return null;
        } else {
            return USER_LEVEL.valueOf(s);
        }
    }
}
