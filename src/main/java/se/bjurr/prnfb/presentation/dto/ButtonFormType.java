package se.bjurr.prnfb.presentation.dto;

import java.util.Locale;

public enum ButtonFormType {
    checkbox,
    input,
    radio,
    textarea;

    public static ButtonFormType fromObject(Object o) {
        String s = o != null ? String.valueOf(o).trim().toLowerCase(Locale.ROOT) : null;
        if (s == null || "".equals(s)) {
            return null;
        } else {
            return ButtonFormType.valueOf(s);
        }
    }
}
