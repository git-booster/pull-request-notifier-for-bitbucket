package se.bjurr.prnfb.service;

import static com.atlassian.bitbucket.pull.PullRequestParticipantStatus.APPROVED;
import static com.atlassian.bitbucket.pull.PullRequestParticipantStatus.NEEDS_WORK;
import static com.atlassian.bitbucket.pull.PullRequestParticipantStatus.UNAPPROVED;
import static java.util.regex.Pattern.compile;
import static se.bjurr.prnfb.http.UrlInvoker.HTTP_METHOD.GET;
import static se.bjurr.prnfb.http.UrlInvoker.urlInvoker;
import static se.bjurr.prnfb.service.RepoProtocol.http;
import static se.bjurr.prnfb.service.RepoProtocol.ssh;

import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.pull.PullRequestParticipantStatus;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryCloneLinksRequest;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.server.ApplicationPropertiesService;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.NamedLink;
import com.atlassian.bitbucket.util.Operation;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import se.bjurr.prnfb.http.ClientKeyStore;
import se.bjurr.prnfb.http.HttpResponse;
import se.bjurr.prnfb.http.Invoker;
import se.bjurr.prnfb.http.UrlInvoker;
import se.bjurr.prnfb.listener.PrnfbPullRequestAction;
import se.bjurr.prnfb.service.PrnfbRenderer.ENCODE_FOR;
import se.bjurr.prnfb.settings.PrnfbNotification;

public enum PrnfbVariable {
  BUTTON_TRIGGER_TITLE(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return getOrEmpty(variables, BUTTON_TRIGGER_TITLE);
        }
      }),
  EVERYTHING_URL(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          final List<String> parts = new ArrayList<>();
          for (final PrnfbVariable v : PrnfbVariable.values()) {
            if (v != EVERYTHING_URL //
                && v != PULL_REQUEST_DESCRIPTION) {
              parts.add(v.name() + "=${" + v.name() + "}");
            }
          }
          Collections.sort(parts);
          return String.join("&", parts);
        }
      }),
  VARIABLE_REGEX_MATCH(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          if (prnfbNotification == null || !prnfbNotification.getVariableName().isPresent()) {
            return "";
          }
          final String variableName = prnfbNotification.getVariableName().get();
          String variableValue = "";
          for (final PrnfbVariable v : PrnfbVariable.values()) {
            if (v.name().equals(variableName) && v != VARIABLE_REGEX_MATCH) {
              variableValue =
                  v.resolve(
                      pullRequest,
                      pullRequestAction,
                      applicationUser,
                      repositoryService,
                      propertiesService,
                      prnfbNotification,
                      variables,
                      clientKeyStore,
                      shouldAcceptAnyCertificate,
                      securityService);
            }
          }
          if (prnfbNotification.getVariableRegex().isPresent()) {
            final Matcher m =
                compile(prnfbNotification.getVariableRegex().get()).matcher(variableValue);
            if (!m.find()) {
              return "";
            }
            if (m.groupCount() == 0) {
              return m.group();
            }
            return m.group(1);
          } else {
            return variableValue;
          }
        }
      }),
  INJECTION_URL_VALUE(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          if (prnfbNotification == null || !prnfbNotification.getInjectionUrl().isPresent()) {
            return "";
          }
          final PrnfbRenderer renderer =
              new PrnfbRenderer(
                  pullRequest,
                  pullRequestAction,
                  applicationUser,
                  repositoryService,
                  propertiesService,
                  prnfbNotification,
                  variables,
                  securityService);
          final String renderedUrlParam =
              renderer.render(
                  prnfbNotification.getInjectionUrl().get(),
                  ENCODE_FOR.URL,
                  clientKeyStore,
                  shouldAcceptAnyCertificate);
          final UrlInvoker urlInvoker =
              urlInvoker() //
                  .withUrlParam(renderedUrlParam) //
                  .withMethod(GET) //
                  .withProxyServer(prnfbNotification.getProxyServer()) //
                  .withProxyPort(prnfbNotification.getProxyPort()) //
                  .withProxySchema(prnfbNotification.getProxySchema()) //
                  .withProxyUser(prnfbNotification.getProxyUser()) //
                  .withProxyPassword(prnfbNotification.getProxyPassword()) //
                  .appendBasicAuth(prnfbNotification) //
                  .withClientKeyStore(clientKeyStore) //
                  .shouldAcceptAnyCertificate(shouldAcceptAnyCertificate);
          createInvoker() //
              .invoke(urlInvoker);
          final String rawResponse = urlInvoker.getResponse().getContent().trim();
          if (prnfbNotification.getInjectionUrlRegexp().isPresent()) {
            final Matcher m =
                compile(prnfbNotification.getInjectionUrlRegexp().get()).matcher(rawResponse);
            if (!m.find()) {
              return "";
            }
            if (m.groupCount() == 0) {
              return m.group();
            }
            return m.group(1);
          } else {
            return rawResponse;
          }
        }
      }),
  PULL_REQUEST_ACTION(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return prnfbPullRequestAction.name();
        }
      }),
  PULL_REQUEST_AUTHOR_DISPLAY_NAME(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getAuthor().getUser().getDisplayName();
        }
      }),
  PULL_REQUEST_AUTHOR_EMAIL(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getAuthor().getUser().getEmailAddress();
        }
      }),
  PULL_REQUEST_AUTHOR_ID(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getAuthor().getUser().getId() + "";
        }
      }),
  PULL_REQUEST_AUTHOR_NAME(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getAuthor().getUser().getName();
        }
      }),
  PULL_REQUEST_AUTHOR_SLUG(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getAuthor().getUser().getSlug();
        }
      }),
  PULL_REQUEST_COMMENT_ACTION(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return getOrEmpty(variables, PULL_REQUEST_COMMENT_ACTION);
        }
      }),
  PULL_REQUEST_COMMENT_TEXT(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return getOrEmpty(variables, PULL_REQUEST_COMMENT_TEXT);
        }
      }),
  PULL_REQUEST_COMMENT_ID(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return getOrEmpty(variables, PULL_REQUEST_COMMENT_ID);
        }
      }),
  PULL_REQUEST_DESCRIPTION(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getDescription();
        }
      }),
  PULL_REQUEST_FROM_BRANCH(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getFromRef().getDisplayId();
        }
      }),
  PULL_REQUEST_FROM_HASH(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getFromRef().getLatestCommit();
        }
      }),
  PULL_REQUEST_PREVIOUS_FROM_HASH(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return getOrEmpty(variables, PULL_REQUEST_PREVIOUS_FROM_HASH);
        }
      }),
  PULL_REQUEST_PREVIOUS_TO_HASH(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return getOrEmpty(variables, PULL_REQUEST_PREVIOUS_TO_HASH);
        }
      }),
  PULL_REQUEST_FROM_HTTP_CLONE_URL(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return cloneUrlFromRepository(
              http,
              pullRequest.getFromRef().getRepository(),
              repositoryService,
              securityService,
              true);
        }
      }),
  PULL_REQUEST_FROM_ID(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getFromRef().getId();
        }
      }),
  PULL_REQUEST_FROM_REPO_ID(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getFromRef().getRepository().getId() + "";
        }
      }),
  PULL_REQUEST_FROM_REPO_NAME(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getFromRef().getRepository().getName() + "";
        }
      }),
  PULL_REQUEST_FROM_REPO_PROJECT_ID(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getFromRef().getRepository().getProject().getId() + "";
        }
      }),
  PULL_REQUEST_FROM_REPO_PROJECT_KEY(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getFromRef().getRepository().getProject().getKey();
        }
      }),
  PULL_REQUEST_FROM_REPO_SLUG(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getFromRef().getRepository().getSlug() + "";
        }
      }),
  PULL_REQUEST_FROM_SSH_CLONE_URL(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return cloneUrlFromRepository(
              ssh,
              pullRequest.getFromRef().getRepository(),
              repositoryService,
              securityService,
              false);
        }
      }),
  PULL_REQUEST_ID(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getId() + "";
        }
      }),
  PULL_REQUEST_MERGE_COMMIT(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfsPullRequestAction,
            final ApplicationUser stashUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfsNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return getOrEmpty(variables, PULL_REQUEST_MERGE_COMMIT);
        }
      }),
  PULL_REQUEST_PARTICIPANTS_APPROVED_COUNT(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return Long.toString(
              pullRequest.getParticipants().stream().filter(p -> p.isApproved()).count());
        }
      }),
  PULL_REQUEST_PARTICIPANTS_EMAIL(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getParticipants()
                  .stream()
                  .map(p -> p.getUser().getEmailAddress())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .map(p -> p.getUser().getDisplayName())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_APPROVED_COUNT(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return Long.toString(
              pullRequest.getReviewers().stream().filter(p -> p.isApproved()).count());
        }
      }),
  PULL_REQUEST_REVIEWERS_EMAIL(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .map(p -> p.getUser().getEmailAddress())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_NEEDS_WORK_SLUG(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .filter(p -> p.getStatus() == NEEDS_WORK)
                  .map(p -> p.getUser().getSlug())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_NEEDS_WORK_EMAIL(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .filter(p -> p.getStatus() == NEEDS_WORK)
                  .map(p -> p.getUser().getEmailAddress())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_NEEDS_WORK_NAME(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .filter(p -> p.getStatus() == NEEDS_WORK)
                  .map(p -> p.getUser().getName())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_NEEDS_WORK_DISPLAY_NAME(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .filter(p -> p.getStatus() == NEEDS_WORK)
                  .map(p -> p.getUser().getDisplayName())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_UNAPPROVED_SLUG(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .filter(p -> p.getStatus() == UNAPPROVED)
                  .map(p -> p.getUser().getSlug())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_UNAPPROVED_EMAIL(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .filter(p -> p.getStatus() == UNAPPROVED)
                  .map(p -> p.getUser().getEmailAddress())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_UNAPPROVED_NAME(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .filter(p -> p.getStatus() == UNAPPROVED)
                  .map(p -> p.getUser().getName())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_UNAPPROVED_DISPLAY_NAME(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .filter(p -> p.getStatus() == UNAPPROVED)
                  .map(p -> p.getUser().getDisplayName())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_APPROVED_SLUG(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .filter(p -> p.getStatus() == APPROVED)
                  .map(p -> p.getUser().getSlug())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_APPROVED_EMAIL(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .filter(p -> p.getStatus() == APPROVED)
                  .map(p -> p.getUser().getEmailAddress())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_APPROVED_NAME(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .filter(p -> p.getStatus() == APPROVED)
                  .map(p -> p.getUser().getName())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_APPROVED_DISPLAY_NAME(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .filter(p -> p.getStatus() == APPROVED)
                  .map(p -> p.getUser().getDisplayName())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_ID(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .map(p -> "" + p.getUser().getId())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_REVIEWERS_SLUG(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {

          return listToString(
              pullRequest
                  .getReviewers()
                  .stream()
                  .map(p -> p.getUser().getSlug())
                  .collect(Collectors.toList()));
        }
      }),
  PULL_REQUEST_STATE(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getState().name();
        }
      }),
  PULL_REQUEST_TITLE(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getTitle();
        }
      }),
  PULL_REQUEST_TO_BRANCH(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getToRef().getDisplayId();
        }
      }),
  PULL_REQUEST_TO_HASH(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getToRef().getLatestCommit();
        }
      }),
  PULL_REQUEST_TO_HTTP_CLONE_URL(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return cloneUrlFromRepository(
              http,
              pullRequest.getToRef().getRepository(),
              repositoryService,
              securityService,
              true);
        }
      }),
  PULL_REQUEST_TO_ID(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getToRef().getId();
        }
      }),
  PULL_REQUEST_TO_REPO_ID(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getToRef().getRepository().getId() + "";
        }
      }),
  PULL_REQUEST_TO_REPO_NAME(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getToRef().getRepository().getName() + "";
        }
      }),
  PULL_REQUEST_TO_REPO_PROJECT_ID(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getToRef().getRepository().getProject().getId() + "";
        }
      }),
  PULL_REQUEST_TO_REPO_PROJECT_KEY(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getToRef().getRepository().getProject().getKey();
        }
      }),
  PULL_REQUEST_TO_REPO_SLUG(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getToRef().getRepository().getSlug() + "";
        }
      }),
  PULL_REQUEST_TO_SSH_CLONE_URL(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return cloneUrlFromRepository(
              ssh,
              pullRequest.getToRef().getRepository(),
              repositoryService,
              securityService,
              false);
        }
      }),
  PULL_REQUEST_URL(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return getPullRequestUrl(propertiesService, pullRequest);
        }
      }),
  PULL_REQUEST_USER_GROUPS(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return getOrEmpty(variables, PULL_REQUEST_USER_GROUPS);
        }
      }),
  PULL_REQUEST_USER_DISPLAY_NAME(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return applicationUser.getDisplayName();
        }
      }),
  PULL_REQUEST_USER_EMAIL_ADDRESS(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return applicationUser.getEmailAddress();
        }
      }),
  PULL_REQUEST_USER_ID(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return applicationUser.getId() + "";
        }
      }),
  PULL_REQUEST_USER_NAME(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return applicationUser.getName();
        }
      }),
  PULL_REQUEST_USER_SLUG(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return applicationUser.getSlug();
        }
      }),
  PULL_REQUEST_VERSION(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction prnfbPullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return pullRequest.getVersion() + "";
        }
      }),
  BUTTON_FORM_DATA(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return getOrEmpty(variables, BUTTON_FORM_DATA);
        }
      }),
  PULL_REQUEST_REVIEWERS_NEEDS_WORK_COUNT(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return countParticipantsWithStatus(pullRequest.getReviewers(), NEEDS_WORK);
        }
      }),
  PULL_REQUEST_REVIEWERS_UNAPPROVED_COUNT(
      new PrnfbVariableResolver() {
        @Override
        public String resolve(
            final PullRequest pullRequest,
            final PrnfbPullRequestAction pullRequestAction,
            final ApplicationUser applicationUser,
            final RepositoryService repositoryService,
            final ApplicationPropertiesService propertiesService,
            final PrnfbNotification prnfbNotification,
            final Map<PrnfbVariable, String> variables,
            final ClientKeyStore clientKeyStore,
            final boolean shouldAcceptAnyCertificate,
            final SecurityService securityService) {
          return countParticipantsWithStatus(pullRequest.getReviewers(), UNAPPROVED);
        }
      });

  private static String countParticipantsWithStatus(
      final Set<PullRequestParticipant> participants, final PullRequestParticipantStatus status) {
    Integer count = 0;
    for (final PullRequestParticipant participant : participants) {
      if (participant.getStatus() == status) {
        count++;
      }
    }
    return count.toString();
  }

  private static Invoker mockedInvoker =
      new Invoker() {
        @Override
        public HttpResponse invoke(final UrlInvoker urlInvoker) {
          return urlInvoker.invoke();
        }
      };

  private static String cloneUrlFromRepository(
      final RepoProtocol protocol,
      final Repository repository,
      final RepositoryService repositoryService,
      final SecurityService securityService,
      final boolean stripUserInfo) {
    return securityService //
        .withPermission(Permission.ADMIN, "cloneUrls") //
        .call(
            new Operation<String, RuntimeException>() {
              @Override
              public String perform() throws RuntimeException {
                final String rawUrlString =
                    getRawUrlString(protocol, repository, repositoryService);
                if (!stripUserInfo) {
                  return rawUrlString;
                }
                try {
                  final URI uri = new URI(rawUrlString);
                  if (uri.getUserInfo() == null) {
                    return rawUrlString;
                  } else {
                    final URI stripped =
                        new URI(
                            uri.getScheme(),
                            null,
                            uri.getHost(),
                            uri.getPort(),
                            uri.getPath(),
                            uri.getQuery(),
                            uri.getFragment());
                    return stripped.toASCIIString();
                  }
                } catch (NullPointerException | URISyntaxException e) {
                  throw new RuntimeException(e);
                }
              }

              private String getRawUrlString(
                  final RepoProtocol protocol,
                  final Repository repository,
                  final RepositoryService repositoryService) {
                final RepositoryCloneLinksRequest request =
                    new RepositoryCloneLinksRequest.Builder() //
                        .protocol(protocol.name()) //
                        .repository(repository) //
                        .build();
                final Set<NamedLink> cloneLinks = repositoryService.getCloneLinks(request);
                final Set<String> allUrls = new TreeSet<>();
                final Iterator<NamedLink> itr = cloneLinks.iterator();
                while (itr.hasNext()) {
                  allUrls.add(itr.next().getHref());
                }
                if (allUrls.isEmpty()) {
                  return "";
                }
                return allUrls.iterator().next();
              }
            });
  }

  private static Invoker createInvoker() {
    if (mockedInvoker != null) {
      return mockedInvoker;
    }
    return new Invoker() {
      @Override
      public HttpResponse invoke(final UrlInvoker urlInvoker) {
        return urlInvoker.invoke();
      }
    };
  }

  private static String getOrEmpty(
      final Map<PrnfbVariable, String> variables, final PrnfbVariable variable) {
    if (variables.get(variable) == null) {
      return "";
    }
    return variables.get(variable);
  }

  private static String getPullRequestUrl(
      final ApplicationPropertiesService propertiesService, final PullRequest pullRequest) {
    return propertiesService.getBaseUrl()
        + "/projects/"
        + pullRequest.getToRef().getRepository().getProject().getKey()
        + "/repos/"
        + pullRequest.getToRef().getRepository().getSlug()
        + "/pull-requests/"
        + pullRequest.getId();
  }

  private static String listToString(final List<String> list) {
    final List<String> copy = new ArrayList<>(list);
    Collections.sort(copy);
    return String.join(",", copy);
  }

  public static void setInvoker(final Invoker invoker) {
    PrnfbVariable.mockedInvoker = invoker;
  }

  private PrnfbVariableResolver resolver;

  PrnfbVariable(final PrnfbVariableResolver resolver) {
    this.resolver = resolver;
  }

  public String resolve(
      final PullRequest pullRequest,
      final PrnfbPullRequestAction pullRequestAction,
      final ApplicationUser applicationUser,
      final RepositoryService repositoryService,
      final ApplicationPropertiesService propertiesService,
      final PrnfbNotification prnfbNotification,
      final Map<PrnfbVariable, String> variables,
      final ClientKeyStore clientKeyStore,
      final boolean shouldAcceptAnyCertificate,
      final SecurityService securityService) {
    return resolver.resolve(
        pullRequest,
        pullRequestAction,
        applicationUser,
        repositoryService,
        propertiesService,
        prnfbNotification,
        variables,
        clientKeyStore,
        shouldAcceptAnyCertificate,
        securityService);
  }
}
