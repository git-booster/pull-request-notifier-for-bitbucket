package se.bjurr.prnfb.settings;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.regex.Pattern.compile;
import static se.bjurr.prnfb.Util.checkNotNull;
import static se.bjurr.prnfb.Util.emptyToNull;
import static se.bjurr.prnfb.Util.firstNotNull;
import static se.bjurr.prnfb.Util.nullToEmpty;
import static se.bjurr.prnfb.http.UrlInvoker.HTTP_METHOD.GET;
import static se.bjurr.prnfb.service.PrnfbRenderer.ENCODE_FOR.NONE;
import static se.bjurr.prnfb.settings.TRIGGER_IF_MERGE.ALWAYS;

import com.atlassian.bitbucket.pull.PullRequestState;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import se.bjurr.prnfb.http.UrlInvoker.HTTP_METHOD;
import se.bjurr.prnfb.listener.PrnfbPullRequestAction;
import se.bjurr.prnfb.service.PrnfbRenderer.ENCODE_FOR;

public class PrnfbNotification implements HasUuid, Restricted {

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

  public PrnfbNotification() {}

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
    final int prime = 31;
    int result = 1;
    result = prime * result + (filterRegexp == null ? 0 : filterRegexp.hashCode());
    result = prime * result + (filterString == null ? 0 : filterString.hashCode());
    result = prime * result + (headers == null ? 0 : headers.hashCode());
    result = prime * result + (httpVersion == null ? 0 : httpVersion.hashCode());
    result = prime * result + (injectionUrl == null ? 0 : injectionUrl.hashCode());
    result = prime * result + (injectionUrlRegexp == null ? 0 : injectionUrlRegexp.hashCode());
    result = prime * result + (variableName == null ? 0 : variableName.hashCode());
    result = prime * result + (variableRegex == null ? 0 : variableRegex.hashCode());
    result = prime * result + (method == null ? 0 : method.hashCode());
    result = prime * result + (name == null ? 0 : name.hashCode());
    result = prime * result + (password == null ? 0 : password.hashCode());
    result = prime * result + (postContent == null ? 0 : postContent.hashCode());
    result = prime * result + (postContentEncoding == null ? 0 : postContentEncoding.hashCode());
    result = prime * result + (projectKey == null ? 0 : projectKey.hashCode());
    result = prime * result + (proxyPassword == null ? 0 : proxyPassword.hashCode());
    result = prime * result + (proxyPort == null ? 0 : proxyPort.hashCode());
    result = prime * result + (proxySchema == null ? 0 : proxySchema.hashCode());
    result = prime * result + (proxyServer == null ? 0 : proxyServer.hashCode());
    result = prime * result + (proxyUser == null ? 0 : proxyUser.hashCode());
    result = prime * result + (repositorySlug == null ? 0 : repositorySlug.hashCode());
    result = prime * result + (triggerIfCanMerge == null ? 0 : triggerIfCanMerge.hashCode());
    result =
        prime * result + (triggerIgnoreStateList == null ? 0 : triggerIgnoreStateList.hashCode());
    result = prime * result + (triggers == null ? 0 : triggers.hashCode());
    result = prime * result + (updatePullRequestRefs ? 1231 : 1237);
    result = prime * result + (url == null ? 0 : url.hashCode());
    result = prime * result + (user == null ? 0 : user.hashCode());
    result = prime * result + (uuid == null ? 0 : uuid.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return "PrnfbNotification [filterRegexp="
        + filterRegexp
        + ", filterString="
        + filterString
        + ", headers="
        + headers
        + ", injectionUrl="
        + injectionUrl
        + ", injectionUrlRegexp="
        + injectionUrlRegexp
        + ", variableName="
        + variableName
        + ", variableRegex="
        + variableRegex
        + ", method="
        + method
        + ", name="
        + name
        + ", password="
        + password
        + ", postContent="
        + postContent
        + ", projectKey="
        + projectKey
        + ", proxyPassword="
        + proxyPassword
        + ", proxyPort="
        + proxyPort
        + ", proxyServer="
        + proxyServer
        + ", proxyUser="
        + proxyUser
        + ", repositorySlug="
        + repositorySlug
        + ", triggerIfCanMerge="
        + triggerIfCanMerge
        + ", triggerIgnoreStateList="
        + triggerIgnoreStateList
        + ", triggers="
        + triggers
        + ", updatePullRequestRefs="
        + updatePullRequestRefs
        + ", url="
        + url
        + ", user="
        + user
        + ", uuid="
        + uuid
        + ", postContentEncoding="
        + postContentEncoding
        + ", proxySchema="
        + proxySchema
        + ", httpVersion="
        + httpVersion
        + "]";
  }

  public ENCODE_FOR getPostContentEncoding() {
    return firstNotNull(this.postContentEncoding, ENCODE_FOR.NONE);
  }

  public String getHttpVersion() {
    return httpVersion;
  }
}
