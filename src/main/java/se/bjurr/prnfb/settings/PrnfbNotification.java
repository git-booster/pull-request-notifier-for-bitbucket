package se.bjurr.prnfb.settings;

import com.atlassian.bitbucket.pull.PullRequestState;
import com.google.common.base.Objects;
import se.bjurr.prnfb.Java2Json;
import se.bjurr.prnfb.Util;
import se.bjurr.prnfb.http.UrlInvoker.HTTP_METHOD;
import se.bjurr.prnfb.listener.PrnfbPullRequestAction;
import se.bjurr.prnfb.service.PrnfbRenderer.ENCODE_FOR;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.regex.Pattern.compile;
import static se.bjurr.prnfb.Util.checkNotNull;
import static se.bjurr.prnfb.Util.emptyToNull;
import static se.bjurr.prnfb.Util.firstNotNull;
import static se.bjurr.prnfb.Util.nullToEmpty;
import static se.bjurr.prnfb.http.HttpUtil.trimOrEmpty;
import static se.bjurr.prnfb.http.UrlInvoker.HTTP_METHOD.GET;
import static se.bjurr.prnfb.service.PrnfbRenderer.ENCODE_FOR.NONE;
import static se.bjurr.prnfb.settings.TRIGGER_IF_MERGE.ALWAYS;

public class PrnfbNotification implements HasUuid, Comparable<PrnfbNotification>, Restricted, Java2Json._2JS {

    private static final String DEFAULT_NAME = "Notification";
    private String filterRegexp;
    private String filterString;
    private List<PrnfbHeader> headers;
    private String injectionUrl;
    private String injectionUrlRegexp;
    private String variableName;
    private String variableRegex;
    private HTTP_METHOD method;
    private String name;
    private String password;
    private String postContent;
    private String projectKey;
    private String proxyPassword;
    private Integer proxyPort;
    private String proxyServer;
    private String proxyUser;
    private String repositorySlug;
    private TRIGGER_IF_MERGE triggerIfCanMerge;
    private List<PullRequestState> triggerIgnoreStateList;
    private List<PrnfbPullRequestAction> triggers;
    private boolean updatePullRequestRefs;
    private String url;
    private String user;
    private UUID uuid;
    private ENCODE_FOR postContentEncoding;
    private String proxySchema;
    private String httpVersion;

    public PrnfbNotification() {
    }

    public Map<String, Object> _2js() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("filterRegexp", this.filterRegexp);
        m.put("filterString", this.filterString);
        m.put("headers", Java2Json._2List((List) this.headers));
        m.put("injectionUrl", this.injectionUrl);
        m.put("injectionUrlRegexp", this.injectionUrlRegexp);
        m.put("variableName", this.variableName);
        m.put("variableRegex", this.variableRegex);
        m.put("method", this.method);
        m.put("name", this.name);
        m.put("password", this.password);
        m.put("postContent", this.postContent);
        m.put("projectKey", this.projectKey);
        m.put("proxyPassword", this.proxyPassword);
        m.put("proxyPort", this.proxyPort);
        m.put("proxyServer", this.proxyServer);
        m.put("proxyUser", this.proxyUser);
        m.put("repositorySlug", this.repositorySlug);
        m.put("triggerIfCanMerge", this.triggerIfCanMerge);
        m.put("triggerIgnoreStateList", Java2Json._2List((List) this.triggerIgnoreStateList));
        m.put("triggers", Java2Json._2List((List) this.triggers));
        m.put("updatePullRequestRefs", this.updatePullRequestRefs);
        m.put("url", this.url);
        m.put("user", this.user);
        m.put("uuid", this.uuid);
        m.put("postContentEncoding", this.postContentEncoding);
        m.put("proxySchema", this.proxySchema);
        m.put("httpVersion", this.httpVersion);
        return m;
    }

    public static PrnfbNotification _fjs(Map<String, Object> m) {
        if (m != null) {
            List<Map<String, Object>> list = (List) m.get("headers");
            List<PrnfbHeader> headers = new ArrayList<>();
            if (list != null) {
                for (Map<String, Object> mm : list) {
                    PrnfbHeader h = PrnfbHeader._fjs(mm);
                    if (h != null) {
                        headers.add(h);
                    }
                }
            }

            List<PullRequestState> states = new ArrayList<>();
            List<String> strings = (List) m.get("triggerIgnoreStateList");
            if (strings != null) {
                for (String s : strings) {
                    s = s != null ? s.trim().toUpperCase(Locale.ROOT) : null;
                    if (s != null) {
                        try {
                            PullRequestState prs = PullRequestState.valueOf(s);
                            states.add(prs);
                        } catch (RuntimeException re) {
                            // ignore... oh well
                        }
                    }
                }
            }

            List<PrnfbPullRequestAction> triggers = new ArrayList<>();
            strings = (List) m.get("triggers");
            if (strings != null) {
                for (String s : strings) {
                    PrnfbPullRequestAction action = PrnfbPullRequestAction.fromObject(s);
                    if (action != null) {
                        triggers.add(action);
                    }
                }
            }

            PrnfbNotification n = new PrnfbNotification();
            n.filterRegexp = (String) m.get("filterRegexp");
            n.filterString = (String) m.get("filterString");
            n.headers = headers;
            n.injectionUrl = (String) m.get("injectionUrl");
            n.injectionUrlRegexp = (String) m.get("injectionUrlRegexp");
            n.variableName = (String) m.get("variableName");
            n.variableRegex = (String) m.get("variableRegex");
            n.method = HTTP_METHOD.fromObject(m.get("method"));
            n.name = (String) m.get("name");
            n.password = (String) m.get("password");
            n.postContent = (String) m.get("postContent");
            n.projectKey = (String) m.get("projectKey");
            n.proxyPassword = (String) m.get("proxyPassword");

            Long l = (Long) m.get("proxyPort");
            if (l != null) {
                n.proxyPort = l.intValue();
            }

            n.proxyServer = (String) m.get("proxyServer");
            n.proxyUser = (String) m.get("proxyUser");
            n.repositorySlug = (String) m.get("repositorySlug");
            n.triggerIfCanMerge = TRIGGER_IF_MERGE.fromObject(m.get("triggerIfCanMerge"));
            n.triggerIgnoreStateList = states;
            n.triggers = triggers;

            Boolean b = (Boolean) m.get("updatePullRequestRefs");
            n.updatePullRequestRefs = b != null ? b : false;
            n.url = (String) m.get("url");
            n.user = (String) m.get("user");
            n.uuid = Util.toUuid(m.get("uuid"));
            n.postContentEncoding = ENCODE_FOR.fromObject(m.get("postContentEncoding"));
            n.proxySchema = (String) m.get("proxySchema");
            n.httpVersion = (String) m.get("httpVersion");
            return n;
        }
        return null;
    }

    public PrnfbNotification(final PrnfbNotificationBuilder builder) throws ValidationException {
        this.uuid = firstNotNull(builder.getUUID(), randomUUID());
        this.proxyUser = emptyToNull(nullToEmpty(builder.getProxyUser()).trim());
        this.proxyPassword = emptyToNull(nullToEmpty(builder.getProxyPassword()).trim());
        this.proxyServer = emptyToNull(nullToEmpty(builder.getProxyServer()).trim());
        this.proxySchema = emptyToNull(nullToEmpty(builder.getProxySchema()).trim());
        this.proxyPort = builder.getProxyPort();
        this.headers = checkNotNull(builder.getHeaders());
        this.postContent = emptyToNull(nullToEmpty(builder.getPostContent()).trim());
        this.method = firstNotNull(builder.getMethod(), GET);
        this.triggerIfCanMerge = firstNotNull(builder.getTriggerIfCanMerge(), ALWAYS);
        this.repositorySlug = emptyToNull(builder.getRepositorySlug());
        this.projectKey = emptyToNull(builder.getProjectKey());
        try {
            new URL(builder.getUrl());
        } catch (final Exception e) {
            throw new ValidationException("url", "URL not valid!");
        }
        if (!nullToEmpty(builder.getFilterRegexp()).trim().isEmpty()) {
            try {
                compile(builder.getFilterRegexp());
            } catch (final Exception e) {
                throw new ValidationException(
                        "filter_regexp", "Filter regexp not valid! " + e.getMessage().replaceAll("\n", " "));
            }
            if (nullToEmpty(builder.getFilterString()).trim().isEmpty()) {
                throw new ValidationException(
                        "filter_string", "Filter string not set, nothing to match regexp against!");
            }
        }
        this.url = builder.getUrl();
        this.user = emptyToNull(nullToEmpty(builder.getUser()).trim());
        this.password = emptyToNull(nullToEmpty(builder.getPassword()).trim());
        this.triggers = checkNotNull(builder.getTriggers());
        if (this.triggers.isEmpty()) {
            throw new ValidationException("triggers", "At least one trigger must be selected.");
        }
        this.updatePullRequestRefs = builder.isUpdatePullRequestRefs();
        this.filterString = emptyToNull(nullToEmpty(builder.getFilterString()).trim());
        this.filterRegexp = emptyToNull(nullToEmpty(builder.getFilterRegexp()).trim());
        this.name = firstNotNull(emptyToNull(nullToEmpty(builder.getName()).trim()), DEFAULT_NAME);
        this.injectionUrl = emptyToNull(nullToEmpty(builder.getInjectionUrl()).trim());
        this.injectionUrlRegexp = emptyToNull(nullToEmpty(builder.getInjectionUrlRegexp()).trim());
        this.variableName = emptyToNull(nullToEmpty(builder.getVariableName()).trim());
        this.variableRegex = emptyToNull(nullToEmpty(builder.getVariableRegex()).trim());
        this.triggerIgnoreStateList = builder.getTriggerIgnoreStateList();
        this.postContentEncoding = firstNotNull(builder.getPostContentEncoding(), NONE);
        this.httpVersion = builder.getHttpVersion();
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

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PrnfbNotification other = (PrnfbNotification) obj;
        if (filterRegexp == null) {
            if (other.filterRegexp != null) {
                return false;
            }
        } else if (!filterRegexp.equals(other.filterRegexp)) {
            return false;
        }
        if (filterString == null) {
            if (other.filterString != null) {
                return false;
            }
        } else if (!filterString.equals(other.filterString)) {
            return false;
        }
        if (headers == null) {
            if (other.headers != null) {
                return false;
            }
        } else if (!headers.equals(other.headers)) {
            return false;
        }
        if (httpVersion == null) {
            if (other.httpVersion != null) {
                return false;
            }
        } else if (!httpVersion.equals(other.httpVersion)) {
            return false;
        }
        if (injectionUrl == null) {
            if (other.injectionUrl != null) {
                return false;
            }
        } else if (!injectionUrl.equals(other.injectionUrl)) {
            return false;
        }
        if (injectionUrlRegexp == null) {
            if (other.injectionUrlRegexp != null) {
                return false;
            }
        } else if (!injectionUrlRegexp.equals(other.injectionUrlRegexp)) {
            return false;
        }
        if (variableName == null) {
            if (other.variableName != null) {
                return false;
            }
        } else if (!variableName.equals(other.variableName)) {
            return false;
        }
        if (variableRegex == null) {
            if (other.variableRegex != null) {
                return false;
            }
        } else if (!variableRegex.equals(other.variableRegex)) {
            return false;
        }
        if (method != other.method) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (password == null) {
            if (other.password != null) {
                return false;
            }
        } else if (!password.equals(other.password)) {
            return false;
        }
        if (postContent == null) {
            if (other.postContent != null) {
                return false;
            }
        } else if (!postContent.equals(other.postContent)) {
            return false;
        }
        if (postContentEncoding != other.postContentEncoding) {
            return false;
        }
        if (projectKey == null) {
            if (other.projectKey != null) {
                return false;
            }
        } else if (!projectKey.equals(other.projectKey)) {
            return false;
        }
        if (proxyPassword == null) {
            if (other.proxyPassword != null) {
                return false;
            }
        } else if (!proxyPassword.equals(other.proxyPassword)) {
            return false;
        }
        if (proxyPort == null) {
            if (other.proxyPort != null) {
                return false;
            }
        } else if (!proxyPort.equals(other.proxyPort)) {
            return false;
        }
        if (proxySchema == null) {
            if (other.proxySchema != null) {
                return false;
            }
        } else if (!proxySchema.equals(other.proxySchema)) {
            return false;
        }
        if (proxyServer == null) {
            if (other.proxyServer != null) {
                return false;
            }
        } else if (!proxyServer.equals(other.proxyServer)) {
            return false;
        }
        if (proxyUser == null) {
            if (other.proxyUser != null) {
                return false;
            }
        } else if (!proxyUser.equals(other.proxyUser)) {
            return false;
        }
        if (repositorySlug == null) {
            if (other.repositorySlug != null) {
                return false;
            }
        } else if (!repositorySlug.equals(other.repositorySlug)) {
            return false;
        }
        if (triggerIfCanMerge != other.triggerIfCanMerge) {
            return false;
        }
        if (triggerIgnoreStateList == null) {
            if (other.triggerIgnoreStateList != null) {
                return false;
            }
        } else if (!triggerIgnoreStateList.equals(other.triggerIgnoreStateList)) {
            return false;
        }
        if (triggers == null) {
            if (other.triggers != null) {
                return false;
            }
        } else if (!triggers.equals(other.triggers)) {
            return false;
        }
        if (updatePullRequestRefs != other.updatePullRequestRefs) {
            return false;
        }
        if (url == null) {
            if (other.url != null) {
                return false;
            }
        } else if (!url.equals(other.url)) {
            return false;
        }
        if (user == null) {
            if (other.user != null) {
                return false;
            }
        } else if (!user.equals(other.user)) {
            return false;
        }
        if (uuid == null) {
            if (other.uuid != null) {
                return false;
            }
        } else if (!uuid.equals(other.uuid)) {
            return false;
        }
        return true;
    }

    public Optional<String> getFilterRegexp() {
        return ofNullable(this.filterRegexp);
    }

    public Optional<String> getFilterString() {
        return ofNullable(this.filterString);
    }

    public List<PrnfbHeader> getHeaders() {
        return this.headers;
    }

    public Optional<String> getInjectionUrl() {
        return ofNullable(this.injectionUrl);
    }

    public Optional<String> getInjectionUrlRegexp() {
        return ofNullable(this.injectionUrlRegexp);
    }

    public Optional<String> getVariableName() {
        return ofNullable(this.variableName);
    }

    public Optional<String> getVariableRegex() {
        return ofNullable(this.variableRegex);
    }

    public HTTP_METHOD getMethod() {
        return this.method;
    }

    public String getName() {
        return this.name;
    }

    public Optional<String> getPassword() {
        return ofNullable(this.password);
    }

    public Optional<String> getPostContent() {
        return ofNullable(this.postContent);
    }

    @Override
    public Optional<String> getProjectKey() {
        return ofNullable(this.projectKey);
    }

    public Optional<String> getProxyPassword() {
        return ofNullable(this.proxyPassword);
    }

    public Integer getProxyPort() {
        return this.proxyPort;
    }

    public Optional<String> getProxySchema() {
        return ofNullable(this.proxySchema);
    }

    public Optional<String> getProxyServer() {
        return ofNullable(this.proxyServer);
    }

    public Optional<String> getProxyUser() {
        return ofNullable(this.proxyUser);
    }

    @Override
    public Optional<String> getRepositorySlug() {
        return ofNullable(this.repositorySlug);
    }

    public TRIGGER_IF_MERGE getTriggerIfCanMerge() {
        return this.triggerIfCanMerge;
    }

    public List<PullRequestState> getTriggerIgnoreStateList() {
        return this.triggerIgnoreStateList;
    }

    public List<PrnfbPullRequestAction> getTriggers() {
        return this.triggers;
    }

    public boolean isUpdatePullRequestRefs() {
        return this.updatePullRequestRefs;
    }

    public String getUrl() {
        return this.url;
    }

    public Optional<String> getUser() {
        return ofNullable(this.user);
    }

    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : Objects.hashCode(repositorySlug, projectKey, name, url);
    }

    public int compareTo(PrnfbNotification other) {
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
                        String u1 = trimOrEmpty(url);
                        String u2 = trimOrEmpty(other.url);
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

    @Override
    public String toString() {
        return "PR-Notification " + uuid + " " + projectKey + " " + repositorySlug + " " + name;
    }

    public ENCODE_FOR getPostContentEncoding() {
        return firstNotNull(this.postContentEncoding, ENCODE_FOR.NONE);
    }

    public String getHttpVersion() {
        return httpVersion;
    }
}
