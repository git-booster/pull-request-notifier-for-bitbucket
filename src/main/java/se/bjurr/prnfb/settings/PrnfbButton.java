package se.bjurr.prnfb.settings;

import com.google.common.base.Objects;
import se.bjurr.prnfb.Java2Json;
import se.bjurr.prnfb.Util;
import se.bjurr.prnfb.presentation.dto.ON_OR_OFF;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static se.bjurr.prnfb.Util.emptyToNull;
import static se.bjurr.prnfb.Util.firstNotNull;
import static se.bjurr.prnfb.http.HttpUtil.trimOrEmpty;

public class PrnfbButton implements HasUuid, Restricted, Comparable<PrnfbButton>, Java2Json._2JS {

    private ON_OR_OFF confirmation;
    private String name;
    private String projectKey;
    private String repositorySlug;
    private List<PrnfbButtonFormElement> buttonFormElementList;
    private USER_LEVEL userLevel;
    private UUID uuid;
    private String confirmationText;
    private String redirectUrl;

    public Map<String, Object> _2js() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("confirmation", this.confirmation);
        m.put("name", this.name);
        m.put("projectKey", this.projectKey);
        m.put("repositorySlug", this.repositorySlug);
        m.put("buttonFormElementList", Java2Json._2List((List) this.buttonFormElementList));
        m.put("userLevel", this.userLevel);
        m.put("uuid", this.uuid);
        m.put("confirmationText", this.confirmationText);
        m.put("redirectUrl", this.redirectUrl);
        return m;
    }

    public static PrnfbButton _fjs(final Map<String, Object> m) {
        if (m != null) {
            List<Map<String, Object>> list = (List) m.get("buttonFormElementList");
            List<PrnfbButtonFormElement> elements = new ArrayList<>();
            if (list != null) {
                for (Map<String, Object> mm : list) {
                    PrnfbButtonFormElement element = PrnfbButtonFormElement._fjs(mm);
                    if (element != null) {
                        elements.add(element);
                    }
                }
            }

            ON_OR_OFF onOrOff = ON_OR_OFF.fromObject(m.get("confirmation"));
            String name = (String) m.get("name");
            String projectKey = (String) m.get("projectKey");
            String repositorySlug = (String) m.get("repositorySlug");
            USER_LEVEL userLevel = USER_LEVEL.fromObject(m.get("userLevel"));
            UUID uuid = Util.toUuid(m.get("uuid"));
            String confirmationText = (String) m.get("confirmationText");
            String redirectUrl = (String) m.get("redirectUrl");
            return new PrnfbButton(
                    uuid,
                    name,
                    userLevel,
                    onOrOff,
                    projectKey,
                    repositorySlug,
                    confirmationText,
                    redirectUrl,
                    elements);
        }
        return null;
    }

    public PrnfbButton(
            UUID uuid,
            String name,
            USER_LEVEL userLevel,
            ON_OR_OFF confirmation,
            String projectKey,
            String repositorySlug,
            String confirmationText,
            String redirectUrl,
            List<PrnfbButtonFormElement> buttonFormElementList
    ) {
        this.uuid = firstNotNull(uuid, randomUUID());
        this.name = name;
        this.userLevel = userLevel;
        this.confirmation = confirmation;
        this.repositorySlug = emptyToNull(repositorySlug);
        this.projectKey = emptyToNull(projectKey);
        this.confirmationText = emptyToNull(confirmationText);
        this.redirectUrl = emptyToNull(redirectUrl);
        this.buttonFormElementList = firstNotNull(buttonFormElementList, new ArrayList<>());
    }

    public boolean disable() {
        if (projectKey != null && !projectKey.startsWith(".disabled.")) {
            this.projectKey = ".disabled." + this.projectKey;
            return true;
        }
        return false;
    }

    public boolean enable() {
        if (projectKey != null && projectKey.startsWith(".disabled.")) {
            this.projectKey = this.projectKey.substring(".disabled.".length());
            return true;
        }
        return false;
    }

    public String getConfirmationText() {
        return confirmationText;
    }

    public ON_OR_OFF getConfirmation() {
        return this.confirmation;
    }

    public String getName() {
        return this.name;
    }

    public List<PrnfbButtonFormElement> getButtonFormElementList() {
        return buttonFormElementList;
    }

    @Override
    public Optional<String> getProjectKey() {
        return ofNullable(this.projectKey);
    }

    @Override
    public Optional<String> getRepositorySlug() {
        return ofNullable(this.repositorySlug);
    }

    public USER_LEVEL getUserLevel() {
        return this.userLevel;
    }

    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    public String getRedirectUrl() {
        return redirectUrl;
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
        PrnfbButton other = (PrnfbButton) obj;
        if (buttonFormElementList == null) {
            if (other.buttonFormElementList != null) {
                return false;
            }
        } else if (!buttonFormElementList.equals(other.buttonFormElementList)) {
            return false;
        }
        if (confirmation != other.confirmation) {
            return false;
        }
        if (confirmationText == null) {
            if (other.confirmationText != null) {
                return false;
            }
        } else if (!confirmationText.equals(other.confirmationText)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (projectKey == null) {
            if (other.projectKey != null) {
                return false;
            }
        } else if (!projectKey.equals(other.projectKey)) {
            return false;
        }
        if (repositorySlug == null) {
            if (other.repositorySlug != null) {
                return false;
            }
        } else if (!repositorySlug.equals(other.repositorySlug)) {
            return false;
        }
        if (userLevel != other.userLevel) {
            return false;
        }
        if (uuid == null) {
            if (other.uuid != null) {
                return false;
            }
        } else if (!uuid.equals(other.uuid)) {
            return false;
        }
        if (redirectUrl == null) {
            if (other.redirectUrl != null) {
                return false;
            }
        } else if (!redirectUrl.equals(other.redirectUrl)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : Objects.hashCode(repositorySlug, projectKey, name, redirectUrl);
    }

    @Override
    public String toString() {
        return "PR-Button " + uuid + " " + projectKey + " " + repositorySlug + " " + name;
    }

    public int compareTo(PrnfbButton other) {
        if (this == other) {
            return 0;
        } else if (other == null) {
            return -1;
        }
        String p1 = trimOrEmpty(projectKey);
        String p2 = trimOrEmpty(other.projectKey);
        int c = p1.compareToIgnoreCase(p2);
        if (c == 0) {
            String r1 = trimOrEmpty(repositorySlug);
            String r2 = trimOrEmpty(other.repositorySlug);
            c = r1.compareToIgnoreCase(r2);
            if (c == 0) {
                String n1 = trimOrEmpty(name);
                String n2 = trimOrEmpty(other.name);
                c = n1.compareToIgnoreCase(n2);
                if (c == 0) {
                    c = n1.compareTo(n2);
                    if (c == 0) {
                        String u1 = trimOrEmpty(redirectUrl);
                        String u2 = trimOrEmpty(other.redirectUrl);
                        c = u1.compareToIgnoreCase(u2);
                        if (c == 0) {
                            c = u1.compareTo(u2);
                            if (c == 0 && uuid != other.uuid) {
                                if (uuid != null && other.uuid != null) {
                                    c = uuid.toString().compareToIgnoreCase(other.uuid.toString());
                                } else if (uuid != null) {
                                    c = 1;
                                } else {
                                    c = -1;
                                }
                            }
                        }
                    }
                }
            }
        }
        return c;
    }

}
