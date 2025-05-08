package se.bjurr.prnfb.service;

import static se.bjurr.prnfb.Util.isNullOrEmpty;
import static se.bjurr.prnfb.service.PrnfbVariable.BUTTON_FORM_DATA;
import static se.bjurr.prnfb.service.PrnfbVariable.BUTTON_TRIGGER_TITLE;
import static se.bjurr.prnfb.service.PrnfbVariable.PULL_REQUEST_COMMENT_ACTION;
import static se.bjurr.prnfb.service.PrnfbVariable.PULL_REQUEST_COMMENT_ID;
import static se.bjurr.prnfb.service.PrnfbVariable.PULL_REQUEST_COMMENT_TEXT;
import static se.bjurr.prnfb.service.PrnfbVariable.PULL_REQUEST_MERGE_COMMIT;
import static se.bjurr.prnfb.service.PrnfbVariable.PULL_REQUEST_PREVIOUS_FROM_HASH;
import static se.bjurr.prnfb.service.PrnfbVariable.PULL_REQUEST_PREVIOUS_TO_HASH;
import static se.bjurr.prnfb.service.PrnfbVariable.PULL_REQUEST_USER_GROUPS;

import com.atlassian.bitbucket.event.pull.PullRequestCommentEvent;
import com.atlassian.bitbucket.event.pull.PullRequestEvent;
import com.atlassian.bitbucket.event.pull.PullRequestMergedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestRescopedEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import se.bjurr.prnfb.settings.PrnfbButton;

/**
 * {@link PrnfbVariable} is becoming a bit messy with a lot of parameters to resolve different
 * variables. This is intended to replace all those parameters.
 */
public class VariablesContext {

  public static class VariablesContextBuilder {
    public PrnfbButton button;
    public PullRequestEvent pullRequestEvent;
    public String formData;
    public List<String> groups;

    public VariablesContextBuilder setButton(PrnfbButton button) {
      this.button = button;
      return this;
    }

    public VariablesContextBuilder setFormData(String formData) {
      this.formData = formData;
      return this;
    }

    public VariablesContextBuilder setGroups(List<String> groups) {
      this.groups = groups;
      return this;
    }

    public VariablesContextBuilder setPullRequestEvent(PullRequestEvent pullRequestEvent) {
      this.pullRequestEvent = pullRequestEvent;
      return this;
    }

    public VariablesContextBuilder() {}

    public VariablesContext build() {
      return new VariablesContext(this);
    }
  }

  private final PrnfbButton button;
  private final PullRequestEvent pullRequestEvent;
  private final String formData;
  private final List<String> groups;

  public VariablesContext(VariablesContextBuilder b) {
    this.button = b.button;
    this.pullRequestEvent = b.pullRequestEvent;
    this.formData = b.formData;
    this.groups = b.groups;
  }

  public List<String> getGroups() {
    return groups;
  }

  public Map<PrnfbVariable, String> getVariables() {
    final Map<PrnfbVariable, String> variables = new HashMap<>();

    if (groups != null) {
      variables.put(PULL_REQUEST_USER_GROUPS, String.join(",", groups));
    }

    if (button != null) {
      variables.put(BUTTON_TRIGGER_TITLE, button.getName());
    }

    if (!isNullOrEmpty(formData)) {
      variables.put(BUTTON_FORM_DATA, formData);
    }

    if (pullRequestEvent != null) {
      if (pullRequestEvent instanceof PullRequestCommentEvent) {
        final PullRequestCommentEvent pullRequestCommentEvent =
            (PullRequestCommentEvent) pullRequestEvent;
        variables.put(PULL_REQUEST_COMMENT_TEXT, pullRequestCommentEvent.getComment().getText());
        variables.put(
            PULL_REQUEST_COMMENT_ACTION, pullRequestCommentEvent.getCommentAction().name());
        variables.put(PULL_REQUEST_COMMENT_ID, pullRequestCommentEvent.getComment().getId() + "");
      } else if (pullRequestEvent instanceof PullRequestRescopedEvent) {
        final PullRequestRescopedEvent pullRequestRescopedEvent =
            (PullRequestRescopedEvent) pullRequestEvent;
        variables.put(
            PULL_REQUEST_PREVIOUS_FROM_HASH, pullRequestRescopedEvent.getPreviousFromHash());
        variables.put(PULL_REQUEST_PREVIOUS_TO_HASH, pullRequestRescopedEvent.getPreviousToHash());
      } else if (pullRequestEvent instanceof PullRequestMergedEvent) {
        variables.put(
            PULL_REQUEST_MERGE_COMMIT,
            ((PullRequestMergedEvent) pullRequestEvent).getCommit().getId());
      }
    }
    return variables;
  }
}
