package se.bjurr.prnfb.presentation;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static se.bjurr.prnfb.transformer.NotificationTransformer.toNotificationDto;
import static se.bjurr.prnfb.transformer.NotificationTransformer.toNotificationDtoList;
import static se.bjurr.prnfb.transformer.NotificationTransformer.toPrnfbNotification;

import com.atlassian.annotations.security.XsrfProtectionExcluded;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import se.bjurr.prnfb.presentation.dto.NotificationDTO;
import se.bjurr.prnfb.service.SettingsService;
import se.bjurr.prnfb.service.UserCheckService;
import se.bjurr.prnfb.settings.PrnfbNotification;
import se.bjurr.prnfb.settings.USER_LEVEL;

@ExportAsService({NotificationServlet.class})
@Named("NotificationServlet")
@Path("/settings/notifications")
public class NotificationServlet {

  @ComponentImport private final SettingsService settingsService;

  @ComponentImport private final UserCheckService userCheckService;

  @Inject
  public NotificationServlet(SettingsService settingsService, UserCheckService userCheckService) {
    this.settingsService = settingsService;
    this.userCheckService = userCheckService;
  }

  @POST
  @XsrfProtectionExcluded
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Response create(NotificationDTO notificationDto) {
    final USER_LEVEL adminRestriction =
        settingsService.getPrnfbSettingsData().getAdminRestriction();
    if (!this.userCheckService.isAdminAllowed(notificationDto, adminRestriction)) {
      return status(UNAUTHORIZED).build();
    }
    try {
      final PrnfbNotification prnfbNotification = toPrnfbNotification(notificationDto);
      final PrnfbNotification created =
          this.settingsService.addOrUpdateNotification(prnfbNotification);
      final NotificationDTO createdDto = toNotificationDto(created);
      return status(OK) //
          .entity(createdDto) //
          .build();
    } catch (final Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  @DELETE
  @Path("{uuid}")
  @XsrfProtectionExcluded
  @Produces(APPLICATION_JSON)
  public Response delete(@PathParam("uuid") UUID notification) {
    final PrnfbNotification notificationDto = this.settingsService.getNotification(notification);
    final USER_LEVEL adminRestriction =
        settingsService.getPrnfbSettingsData().getAdminRestriction();
    if (!this.userCheckService.isAdminAllowed(notificationDto, adminRestriction)) {
      return status(UNAUTHORIZED).build();
    }
    this.settingsService.deleteNotification(notification);
    return status(OK).build();
  }

  @GET
  @Produces(APPLICATION_JSON)
  public Response get() {
    final List<PrnfbNotification> notifications = this.settingsService.getNotifications();
    final Iterable<PrnfbNotification> notificationsFiltered =
        userCheckService.filterAdminAllowed(notifications);
    final List<NotificationDTO> dtos = toNotificationDtoList(notificationsFiltered);
    Collections.sort(dtos);
    return ok(dtos).build();
  }

  @GET
  @Path("/projects/{projectKey}")
  @Produces(APPLICATION_JSON)
  public Response get(@PathParam("projectKey") String projectKey) {
    final List<PrnfbNotification> notifications = this.settingsService.getNotifications(projectKey);
    final Iterable<PrnfbNotification> notificationsFiltered =
        userCheckService.filterAdminAllowed(notifications);
    final List<NotificationDTO> dtos = toNotificationDtoList(notificationsFiltered);
    Collections.sort(dtos);
    return ok(dtos).build();
  }

  @GET
  @Path("/projects/{projectKey}/repos/{repositorySlug}")
  @Produces(APPLICATION_JSON)
  public Response get(
      @PathParam("projectKey") String projectKey,
      @PathParam("repositorySlug") String repositorySlug) {
    final List<PrnfbNotification> notifications =
        this.settingsService.getNotifications(projectKey, repositorySlug);
    final Iterable<PrnfbNotification> notificationsFiltered =
        userCheckService.filterAdminAllowed(notifications);
    final List<NotificationDTO> dtos = toNotificationDtoList(notificationsFiltered);
    Collections.sort(dtos);
    return ok(dtos).build();
  }

  @GET
  @Path("{uuid}")
  @Produces(APPLICATION_JSON)
  public Response get(@PathParam("uuid") UUID notificationUuid) {
    final PrnfbNotification notification = this.settingsService.getNotification(notificationUuid);
    final USER_LEVEL adminRestriction =
        settingsService.getPrnfbSettingsData().getAdminRestriction();
    if (!this.userCheckService.isAdminAllowed(notification, adminRestriction)) {
      return status(UNAUTHORIZED).build();
    }
    final NotificationDTO dto = toNotificationDto(notification);
    return ok(dto).build();
  }
}
