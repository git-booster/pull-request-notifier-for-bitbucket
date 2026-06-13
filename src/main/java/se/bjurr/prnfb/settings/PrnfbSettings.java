package se.bjurr.prnfb.settings;

import se.bjurr.prnfb.Java2Json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import static se.bjurr.prnfb.Util.checkNotNull;

public class PrnfbSettings implements Java2Json._2JS {

    public static final String UNCHANGED = "KEEP_THIS_TO_LEAVE_UNCHANGED";
    private List<PrnfbButton> buttons;

    private List<PrnfbNotification> notifications = new ArrayList<>();

    private Set<PrnfbNotification> notificationsGlobal = null;
    private Map<String, Set<PrnfbNotification>> notificationsByProj = null;
    private Map<String, Map<String, Set<PrnfbNotification>>> notificationsByRepo = null;
    private Map<UUID, PrnfbNotification> notificationByUUID = null;
    private boolean notificationsSorted = false;

    private Set<PrnfbButton> buttonsGlobal = null;
    private Map<String, Set<PrnfbButton>> buttonsByProj = null;
    private Map<String, Map<String, Set<PrnfbButton>>> buttonsByRepo = null;
    private Map<UUID, PrnfbButton> buttonsByUUID = null;
    private boolean buttonsSorted = false;

    private PrnfbSettingsData prnfbSettingsData;

    public PrnfbSettings() {
    }

    public PrnfbSettings(PrnfbSettingsBuilder builder) {
        this.notifications = checkNotNull(builder.getNotifications());
        this.buttons = checkNotNull(builder.getButtons());
        this.prnfbSettingsData = checkNotNull(builder.getPrnfbSettingsData(), "prnfbSettingsData");
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
        if (list != null) {
            for (Map<String, Object> mm : list) {
                PrnfbButton b = PrnfbButton._fjs(mm);
                if (b != null) {
                    buttons.add(b);
                }
            }
        }

        list = (List) m.get("notifications");
        List<PrnfbNotification> notifications = new ArrayList<>();
        if (list != null) {
            for (Map<String, Object> mm : list) {
                PrnfbNotification n = PrnfbNotification._fjs(mm);
                if (n != null) {
                    notifications.add(n);
                }
            }
        }

        Map<String, Object> mm = (Map) m.get("prnfbSettingsData");
        PrnfbSettingsData data = PrnfbSettingsData._fjs(mm);
        p.buttons = buttons;
        p.notifications = notifications;
        p.prnfbSettingsData = data;
        return p;
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
        if (!buttonsSorted) {
            synchronized (this) {
                if (!buttonsSorted) {
                    Collections.sort(this.buttons);
                    buttonsSorted = true;
                }
            }
        }
        return this.buttons;
    }

    public List<PrnfbNotification> getNotifications() {
        if (!notificationsSorted) {
            synchronized (this) {
                if (!notificationsSorted) {
                    Collections.sort(this.notifications);
                    notificationsSorted = true;
                }
            }
        }
        return this.notifications;
    }

    public PrnfbNotification getNotificationByUUID(UUID uuid) {
        if (notificationByUUID == null) {
            Map<UUID, PrnfbNotification> m = new HashMap<>();
            for (PrnfbNotification n : notifications) {
                UUID u = n.getUuid();
                m.putIfAbsent(u, n);
            }
            this.notificationByUUID = m;
        }
        return notificationByUUID.get(uuid);
    }

    public Set<PrnfbNotification> getNotificationsGlobal() {
        if (notificationsGlobal == null) {
            Set<PrnfbNotification> set = new TreeSet<>();
            for (PrnfbNotification n : notifications) {
                boolean hasR = n.getRepositorySlug().isPresent();
                boolean hasP = n.getProjectKey().isPresent();
                if (!hasR && !hasP) {
                    set.add(n);
                }
            }
            this.notificationsGlobal = set;
        }
        return notificationsGlobal;
    }

    public Set<PrnfbNotification> getNotificationsByRepo(String projectKey, String repoSlug) {
        if (notificationsByRepo == null) {
            Map<String, Map<String, Set<PrnfbNotification>>> m = new TreeMap<>();
            for (PrnfbNotification n : notifications) {
                boolean hasP = n.getProjectKey().isPresent();
                boolean hasR = n.getRepositorySlug().isPresent();
                if (hasP && hasR) {
                    String pKey = n.getProjectKey().get();
                    String rKey = n.getRepositorySlug().get();
                    Map<String, Set<PrnfbNotification>> mm = m.computeIfAbsent(pKey, k -> new TreeMap<>());
                    Set<PrnfbNotification> set = mm.computeIfAbsent(rKey, k -> new TreeSet<>());
                    set.add(n);
                }
            }
            this.notificationsByRepo = m;
        }
        Map<String, Set<PrnfbNotification>> m = notificationsByRepo.get(projectKey);
        if (m != null) {
            Set<PrnfbNotification> set = m.get(repoSlug);
            if (set != null) {
                return set;
            }
        }
        return Collections.emptySet();
    }

    public Set<PrnfbNotification> getNotificationsByProj(String projectKey, boolean includeRepoLevel) {
        if (notificationsByProj == null) {
            Map<String, Set<PrnfbNotification>> m = new TreeMap<>();
            for (PrnfbNotification n : notifications) {
                boolean hasR = n.getRepositorySlug().isPresent();
                boolean hasP = n.getProjectKey().isPresent();
                if (!hasR && hasP) {
                    String pKey = n.getProjectKey().get();
                    Set<PrnfbNotification> set = m.computeIfAbsent(pKey, k -> new TreeSet<>());
                    set.add(n);
                }
            }
            this.notificationsByProj = m;
        }
        Set<PrnfbNotification> set = notificationsByProj.get(projectKey);
        if (includeRepoLevel) {
            TreeSet<PrnfbNotification> includingReposSet = new TreeSet<>();
            if (set != null) {
                includingReposSet.addAll(set);
            }

            // Just to initialize the Map in case getNotificationsByRepo() hasn't been called yet.
            if (notificationsByRepo == null) {
                getNotificationsByRepo("", "");
            }

            Map<String, Set<PrnfbNotification>> m = notificationsByRepo.get(projectKey);
            if (m != null) {
                for (Map.Entry<String, Set<PrnfbNotification>> entry : m.entrySet()) {
                    Set<PrnfbNotification> values = entry.getValue();
                    includingReposSet.addAll(values);
                }
            }
            set = includingReposSet;
        }

        if (set != null) {
            return set;
        }
        return Collections.emptySet();
    }

    public PrnfbButton getButtonByUUID(UUID uuid) {
        if (buttonsByUUID == null) {
            Map<UUID, PrnfbButton> m = new HashMap<>();
            for (PrnfbButton b : buttons) {
                UUID u = b.getUuid();
                m.putIfAbsent(u, b);
            }
            this.buttonsByUUID = m;
        }
        return buttonsByUUID.get(uuid);
    }

    public Set<PrnfbButton> getButtonsGlobal() {
        if (buttonsGlobal == null) {
            Set<PrnfbButton> set = new TreeSet<>();
            for (PrnfbButton b : buttons) {
                boolean hasR = b.getRepositorySlug().isPresent();
                boolean hasP = b.getProjectKey().isPresent();
                if (!hasR && !hasP) {
                    set.add(b);
                }
            }
            this.buttonsGlobal = set;
        }
        return buttonsGlobal;
    }

    public Set<PrnfbButton> getButtonsByRepo(String projectKey, String repoSlug) {
        if (buttonsByRepo == null) {
            Map<String, Map<String, Set<PrnfbButton>>> m = new TreeMap<>();
            for (PrnfbButton b : buttons) {
                boolean hasP = b.getProjectKey().isPresent();
                boolean hasR = b.getRepositorySlug().isPresent();
                if (hasP && hasR) {
                    String pKey = b.getProjectKey().get();
                    String rKey = b.getRepositorySlug().get();
                    Map<String, Set<PrnfbButton>> mm = m.computeIfAbsent(pKey, k -> new TreeMap<>());
                    Set<PrnfbButton> set = mm.computeIfAbsent(rKey, k -> new TreeSet<>());
                    set.add(b);
                }
            }
            this.buttonsByRepo = m;
        }
        Map<String, Set<PrnfbButton>> m = buttonsByRepo.get(projectKey);
        if (m != null) {
            Set<PrnfbButton> set = m.get(repoSlug);
            if (set != null) {
                return set;
            }
        }
        return Collections.emptySet();
    }

    public Set<PrnfbButton> getButtonsByProj(String projectKey, boolean includeRepoLevel) {
        if (buttonsByProj == null) {
            Map<String, Set<PrnfbButton>> m = new TreeMap<>();
            for (PrnfbButton b : buttons) {
                boolean hasR = b.getRepositorySlug().isPresent();
                boolean hasP = b.getProjectKey().isPresent();
                if (!hasR && hasP) {
                    String pKey = b.getProjectKey().get();
                    Set<PrnfbButton> set = m.computeIfAbsent(pKey, k -> new TreeSet<>());
                    set.add(b);
                }
            }
            this.buttonsByProj = m;
        }
        Set<PrnfbButton> set = buttonsByProj.get(projectKey);
        if (includeRepoLevel) {
            TreeSet<PrnfbButton> includingReposSet = new TreeSet<>();
            if (set != null) {
                includingReposSet.addAll(set);
            }

            // Just to initialize the Map in case getButtonsByRepo() hasn't been called yet.
            if (buttonsByRepo == null) {
                getButtonsByRepo("", "");
            }

            Map<String, Set<PrnfbButton>> m = buttonsByRepo.get(projectKey);
            if (m != null) {
                for (Map.Entry<String, Set<PrnfbButton>> entry : m.entrySet()) {
                    Set<PrnfbButton> values = entry.getValue();
                    includingReposSet.addAll(values);
                }
            }
            set = includingReposSet;
        }

        if (set != null) {
            return set;
        }
        return Collections.emptySet();
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
        result = prime * result + (this.prnfbSettingsData == null ? 0 : this.prnfbSettingsData.hashCode());
        return result;
    }
}
