package se.bjurr.prnfb.presentation;

import com.atlassian.annotations.security.XsrfProtectionExcluded;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import se.bjurr.prnfb.presentation.dto.SettingsDataDTO;
import se.bjurr.prnfb.service.SettingsService;
import se.bjurr.prnfb.service.UserCheckService;
import se.bjurr.prnfb.settings.PrnfbSettingsData;
import se.bjurr.prnfb.settings.Restricted;
import se.bjurr.prnfb.settings.USER_LEVEL;

import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static jakarta.ws.rs.core.Response.noContent;
import static jakarta.ws.rs.core.Response.ok;
import static jakarta.ws.rs.core.Response.status;
import static se.bjurr.prnfb.transformer.SettingsTransformer.toDto;
import static se.bjurr.prnfb.transformer.SettingsTransformer.toPrnfbSettingsData;

@ExportAsService({SettingsDataServlet.class})
@Named("SettingsDataServlet")
@Path("/settings")
public class SettingsDataServlet {

    @ComponentImport
    private final SettingsService settingsService;
    @ComponentImport
    private final UserCheckService userCheckService;

    @Inject
    public SettingsDataServlet(UserCheckService userCheckService, SettingsService settingsService) {
        this.userCheckService = userCheckService;
        this.settingsService = settingsService;
    }

    @GET
    @Produces(APPLICATION_JSON)
    public Response get() {
        if (!userCheckService.isViewAllowed()) {
            return status(UNAUTHORIZED).build();
        }

        PrnfbSettingsData settingsData = settingsService.getPrnfbSettingsData();
        SettingsDataDTO settingsDataDto = toDto(settingsData);
        return ok(settingsDataDto).build();
    }

    @POST
    @XsrfProtectionExcluded
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response post(SettingsDataDTO settingsDataDto) {
         USER_LEVEL adminRestriction = settingsService.getPrnfbSettingsData().getAdminRestriction();
        if (!userCheckService.isAdminAllowed(
                new Restricted() {
                    @Override
                    public Optional<String> getRepositorySlug() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> getProjectKey() {
                        return Optional.empty();
                    }
                },
                adminRestriction)) {
            return status(UNAUTHORIZED).build();
        }

        PrnfbSettingsData prnfbSettingsData = toPrnfbSettingsData(settingsDataDto);
        settingsService.setPrnfbSettingsData(prnfbSettingsData);
        return noContent().build();
    }
}
