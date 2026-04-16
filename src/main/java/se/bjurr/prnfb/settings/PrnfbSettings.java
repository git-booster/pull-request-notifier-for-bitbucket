package se.bjurr.prnfb.settings;

import se.bjurr.prnfb.Java2Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static se.bjurr.prnfb.Util.checkNotNull;

public class PrnfbSettings implements Java2Json._2JS {

    public static final String UNCHANGED = "KEEP_THIS_TO_LEAVE_UNCHANGED";
    private List<PrnfbButton> buttons;
    private List<PrnfbNotification> notifications = new ArrayList<>();
    private PrnfbSettingsData prnfbSettingsData;

    public PrnfbSettings() {
    }

    public Map<String, Object> _2js() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("buttons", Java2Json._2List((List) this.buttons));
        m.put("notifications", Java2Json._2List((List) this.notifications));
        m.put("prnfbSettingsData", prnfbSettingsData._2js());
        return m;
    }

    public static PrnfbSettings _fjs(Map<String, Object> m) {
        PrnfbSettings p = new PrnfbSettings();

        List<Map<String, Object>> list = (List) m.get("buttons");
        List<PrnfbButton> buttons = new ArrayList<>();
        for (Map<String, Object> mm : list) {
            PrnfbButton b = PrnfbButton._fjs(mm);
            if (b != null) {
                buttons.add(b);
            }
        }

        list = (List) m.get("notifications");
        List<PrnfbNotification> notifications = new ArrayList<>();
        for (Map<String, Object> mm : list) {
            PrnfbNotification n = PrnfbNotification._fjs(mm);
            if (n != null) {
                notifications.add(n);
            }
        }

        Map<String, Object> mm = (Map) m.get("prnfbSettingsData");
        PrnfbSettingsData data = PrnfbSettingsData._fjs(mm);
        p.buttons = buttons;
        p.notifications = notifications;
        p.prnfbSettingsData = data;
        return p;
    }

    public PrnfbSettings(PrnfbSettingsBuilder builder) {
        this.notifications = checkNotNull(builder.getNotifications());
        this.buttons = checkNotNull(builder.getButtons());
        this.prnfbSettingsData = checkNotNull(builder.getPrnfbSettingsData(), "prnfbSettingsData");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PrnfbSettings other = (PrnfbSettings) obj;
        if (this.buttons == null) {
            if (other.buttons != null) {
                return false;
            }
        } else if (!this.buttons.equals(other.buttons)) {
            return false;
        }
        if (this.notifications == null) {
            if (other.notifications != null) {
                return false;
            }
        } else if (!this.notifications.equals(other.notifications)) {
            return false;
        }
        if (this.prnfbSettingsData == null) {
            if (other.prnfbSettingsData != null) {
                return false;
            }
        } else if (!this.prnfbSettingsData.equals(other.prnfbSettingsData)) {
            return false;
        }
        return true;
    }

    public List<PrnfbButton> getButtons() {
        return this.buttons;
    }

    public List<PrnfbNotification> getNotifications() {
        return this.notifications;
    }

    public PrnfbSettingsData getPrnfbSettingsData() {
        return this.prnfbSettingsData;
    }

    public void setButtons(List<PrnfbButton> list) {
        this.buttons = list;
    }

    public void setNotifications(List<PrnfbNotification> list) {
        this.notifications = list;
    }

    public void setPrnfbSettingsData(PrnfbSettingsData data) {
        this.prnfbSettingsData = data;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.buttons == null ? 0 : this.buttons.hashCode());
        result = prime * result + (this.notifications == null ? 0 : this.notifications.hashCode());
        result =
                prime * result + (this.prnfbSettingsData == null ? 0 : this.prnfbSettingsData.hashCode());
        return result;
    }
}
