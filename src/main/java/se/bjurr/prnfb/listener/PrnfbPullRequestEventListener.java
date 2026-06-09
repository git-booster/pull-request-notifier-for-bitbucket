package se.bjurr.prnfb.listener;

import com.atlassian.bitbucket.event.pull.PullRequestCommentAddedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentDeletedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentEditedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentRepliedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestDeclinedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestDeletedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestEvent;
import com.atlassian.bitbucket.event.pull.PullRequestMergedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestParticipantStatusUpdatedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestReopenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestRescopedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestUpdatedEvent;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.Operation;
import com.atlassian.event.api.EventListener;
import com.atlassian.plugin.event.events.PluginDisablingEvent;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import se.bjurr.prnfb.http.ClientKeyStore;
import se.bjurr.prnfb.http.HttpResponse;
import se.bjurr.prnfb.http.HttpUtil;
import se.bjurr.prnfb.http.NotificationResponse;
import se.bjurr.prnfb.http.UrlInvoker;
import se.bjurr.prnfb.service.PrnfbRenderer;
import se.bjurr.prnfb.service.PrnfbRenderer.ENCODE_FOR;
import se.bjurr.prnfb.service.PrnfbRendererFactory;
import se.bjurr.prnfb.service.SettingsService;
import se.bjurr.prnfb.service.VariablesContext;
import se.bjurr.prnfb.service.VariablesContext.VariablesContextBuilder;
import se.bjurr.prnfb.settings.PrnfbHeader;
import se.bjurr.prnfb.settings.PrnfbNotification;
import se.bjurr.prnfb.settings.PrnfbSettingsData;
import se.bjurr.prnfb.settings.TRIGGER_IF_MERGE;

import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.atlassian.bitbucket.permission.Permission.ADMIN;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.regex.Pattern.compile;
import static org.slf4j.LoggerFactory.getLogger;
import static se.bjurr.prnfb.http.UrlInvoker.urlInvoker;
import static se.bjurr.prnfb.listener.PrnfbPullRequestAction.fromPullRequestEvent;
import static se.bjurr.prnfb.settings.TRIGGER_IF_MERGE.ALWAYS;
import static se.bjurr.prnfb.settings.TRIGGER_IF_MERGE.CONFLICTING;
import static se.bjurr.prnfb.settings.TRIGGER_IF_MERGE.NOT_CONFLICTING;

@Named("PrnfbPullRequestEventListener")
@Component
public class PrnfbPullRequestEventListener {

    private static final Logger LOG = getLogger(PrnfbPullRequestEventListener.class);

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private final PrnfbRendererFactory prnfbRendererFactory;
    private final PullRequestService pullRequestService;
    private final SecurityService securityService;
    private final SettingsService settingsService;

    // core = 20
    // max = 200
    // queue = 50
    // keepalive = 25 seconds
    // do a graceful shutdown

    public PrnfbPullRequestEventListener(
            PrnfbRendererFactory prnfbRendererFactory,
            PullRequestService pullRequestService,
            SettingsService settingsService,
            SecurityService securityService
    ) {
        this.prnfbRendererFactory = prnfbRendererFactory;
        this.pullRequestService = pullRequestService;
        this.settingsService = settingsService;
        this.securityService = securityService;
    }

    private void handleEvent(final PullRequestEvent pullRequestEvent) {
        PullRequest pullRequest = pullRequestEvent.getPullRequest();
        PrnfbSettingsData settings = settingsService.getPrnfbSettingsData();
        ClientKeyStore clientKeyStore = new ClientKeyStore(settings);
        if (pullRequest.isClosed() && pullRequestEvent instanceof PullRequestCommentEvent) {
            return;
        }
        for (PrnfbNotification notification : settingsService.getNotifications()) {
            try {
                handleEventNotification(pullRequestEvent, settings, clientKeyStore, notification);
            } catch (final Exception e) {
                LOG.error("Unable to handle notification " + notification.getUuid() + " " + notification.getName(), e);
            }
        }
    }

    private void handleEventNotification(
            PullRequestEvent pullRequestEvent,
            PrnfbSettingsData settings,
            ClientKeyStore clientKeyStore,
            PrnfbNotification notification
    ) {
        final PrnfbPullRequestAction action = fromPullRequestEvent(pullRequestEvent, notification);
        final VariablesContext variables = new VariablesContextBuilder().setPullRequestEvent(pullRequestEvent).build();
        final PrnfbRenderer renderer = prnfbRendererFactory.create(
                pullRequestEvent.getPullRequest(),
                action,
                notification,
                variables,
                pullRequestEvent.getUser()
        );

        // Might as well run it async here, since we throw away the returned object.
        Runnable r = new Runnable() {
            @Override
            public void run() {
                PrnfbPullRequestEventListener.this.notify(
                        notification,
                        action,
                        pullRequestEvent.getPullRequest(),
                        renderer,
                        clientKeyStore,
                        settings.isShouldAcceptAnyCertificate()
                );
            }
        };
        executorService.execute(r);

    }

    public void handleEventAsync(final PullRequestEvent pullRequestEvent) {
        handleEvent(pullRequestEvent);
    }

    public boolean ignoreBecauseOfConflicting(TRIGGER_IF_MERGE ifMerge, boolean isConflicted) {
        return ifMerge == NOT_CONFLICTING && isConflicted || ifMerge == CONFLICTING && !isConflicted;
    }

    public boolean isNotificationTriggeredByAction(
            final PrnfbNotification notification,
            final PrnfbPullRequestAction pullRequestAction,
            final PrnfbRenderer renderer,
            final PullRequest pullRequest,
            final ClientKeyStore clientKeyStore,
            final Boolean shouldAcceptAnyCertificate
    ) {
        if (!notification.getTriggers().contains(pullRequestAction)) {
            return FALSE;
        }
        if (notification.getProjectKey().isPresent()) {
            if (!notification
                    .getProjectKey()
                    .get()
                    .equals(pullRequest.getToRef().getRepository().getProject().getKey())) {
                return FALSE;
            }
        }
        if (notification.getRepositorySlug().isPresent()) {
            if (!notification
                    .getRepositorySlug()
                    .get()
                    .equals(pullRequest.getToRef().getRepository().getSlug())) {
                return FALSE;
            }
        }
        if (notification.getFilterRegexp().isPresent()
                && notification.getFilterString().isPresent()
                && !compile(notification.getFilterRegexp().get()).matcher(
                renderer.render(
                        notification.getFilterString().get(),
                        ENCODE_FOR.NONE,
                        clientKeyStore,
                        shouldAcceptAnyCertificate
                )
        ).find()) {
            return FALSE;
        }
        if (notification.getTriggerIgnoreStateList().contains(pullRequest.getState())) {
            return FALSE;
        }
        if (notification.getTriggerIfCanMerge() != ALWAYS) {
            // Cannot perform canMerge unless PR is open
            final boolean notYetMerged = pullRequest.isOpen();
            final boolean isConflicted = notYetMerged && hasConflicts(pullRequest);
            if (ignoreBecauseOfConflicting(notification.getTriggerIfCanMerge(), isConflicted)) {
                return FALSE;
            }
        }
        return TRUE;
    }

    private boolean hasConflicts(final PullRequest pr) {
        return securityService.withPermission(ADMIN, "Can merge").call(
                new Operation<Boolean, RuntimeException>() {
                    @Override
                    public Boolean perform() throws RuntimeException {
                        return pullRequestService.canMerge(
                                pr.getToRef().getRepository().getId(), pr.getId()
                        ).isConflicted();
                    }
                }
        );
    }

    public NotificationResponse notify(
            final PrnfbNotification notification,
            final PrnfbPullRequestAction pullRequestAction,
            final PullRequest pullRequest,
            final PrnfbRenderer renderer,
            final ClientKeyStore clientKeyStore,
            final Boolean acceptAny) {
        if (!isNotificationTriggeredByAction(
                notification, pullRequestAction, renderer, pullRequest, clientKeyStore, acceptAny
        )) {
            return null;
        }
        Optional<String> postContent = Optional.empty();
        if (notification.getPostContent().isPresent()) {
            final ENCODE_FOR encodePostContentFor = notification.getPostContentEncoding();
            postContent = Optional.of(renderer.render(
                    notification.getPostContent().get(), encodePostContentFor, clientKeyStore, acceptAny
            ));
        }
        final String renderedUrl = renderer.render(
                notification.getUrl(), ENCODE_FOR.URL, clientKeyStore, acceptAny
        );
        LOG.info(
                notification.getName() + " > " + pullRequest.getFromRef().getId() + "("
                        + pullRequest.getFromRef().getLatestCommit() + ") -> "
                        + pullRequest.getToRef().getId() + "("
                        + pullRequest.getToRef().getLatestCommit() + ")" + " " + renderedUrl
        );
        final UrlInvoker urlInvoker = urlInvoker()
                .withClientKeyStore(clientKeyStore)
                .withUrlParam(renderedUrl)
                .withMethod(notification.getMethod())
                .withPostContent(postContent)
                .appendBasicAuth(notification);
        for (PrnfbHeader header : notification.getHeaders()) {
            urlInvoker.withHeader(header.getName(), renderer.render(
                    header.getValue(), ENCODE_FOR.NONE, clientKeyStore, acceptAny
            ));
        }

        String method = notification.getMethod().toString();
        String err = null;
        HttpResponse httpResponse = null;
        HttpUtil.incrementNotification(notification.getUuid());
        try {
            httpResponse = urlInvoker
                    .withProxyServer(notification.getProxyServer())
                    .withProxyPort(notification.getProxyPort())
                    .withProxySchema(notification.getProxySchema())
                    .withProxyUser(notification.getProxyUser())
                    .withProxyPassword(notification.getProxyPassword())
                    .shouldAcceptAnyCertificate(acceptAny)
                    .setHttpVersion(notification.getHttpVersion()
                    ).invoke();
        } catch (Exception e) {
            Throwable t = e.getCause();
            if (t == null) {
                t = e;
            }
            err = method + " to [" + renderedUrl + "] failed. " + t.getMessage() + " (" + t.getClass() + ")";
        }

        return new NotificationResponse(
                notification.getUuid(), notification.getName(), httpResponse, err
        );
    }

    @EventListener
    public void onEvent(final PullRequestParticipantStatusUpdatedEvent e) {
        handleEventAsync(e);
    }

    @EventListener
    public void onEvent(final PullRequestCommentAddedEvent e) {
        handleEventAsync(e);
    }

    @EventListener
    public void onEvent(final PullRequestCommentDeletedEvent e) {
        handleEventAsync(e);
    }

    @EventListener
    public void onEvent(final PullRequestCommentEditedEvent e) {
        handleEventAsync(e);
    }

    @EventListener
    public void onEvent(final PullRequestCommentRepliedEvent e) {
        handleEventAsync(e);
    }

    @EventListener
    public void onEvent(final PullRequestDeletedEvent e) {
        handleEventAsync(e);
    }

    @EventListener
    public void onEvent(final PullRequestDeclinedEvent e) {
        handleEventAsync(e);
    }

    @EventListener
    public void onEvent(final PullRequestMergedEvent e) {
        handleEventAsync(e);
    }

    @EventListener
    public void onEvent(final PullRequestOpenedEvent e) {
        handleEventAsync(e);
    }

    @EventListener
    public void onEvent(final PullRequestReopenedEvent e) {
        handleEventAsync(e);
    }

    @EventListener
    public void onEvent(final PullRequestRescopedEvent e) {
        handleEventAsync(e);
    }

    @EventListener
    public void onEvent(final PullRequestUpdatedEvent e) {
        handleEventAsync(e);
    }


    // This is the important one (onPluginDisabling) that actually gets invoked on shutdown!
    @EventListener
    public void onPluginDisabling(final PluginDisablingEvent event) {
        executorService.shutdown();
    }


}
