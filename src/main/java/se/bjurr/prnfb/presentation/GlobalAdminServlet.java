package se.bjurr.prnfb.presentation;

import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.project.ProjectService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.templaterenderer.TemplateRenderer;
import se.bjurr.prnfb.Java2Json;
import se.bjurr.prnfb.Util;
import se.bjurr.prnfb.http.HttpUtil;
import se.bjurr.prnfb.service.UserCheckService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static java.util.Optional.empty;
import static se.bjurr.prnfb.Util.immutableMap;
import static se.bjurr.prnfb.service.SettingsService.SETTINGS_STORAGE_KEY;

@ExportAsService({GlobalAdminServlet.class})
@Named("GlobalAdminServlet")
public class GlobalAdminServlet extends HttpServlet {
    private static final long serialVersionUID = 3846987953228399693L;

    @ComponentImport
    private final LoginUriProvider loginUriProvider;
    @ComponentImport
    private final TemplateRenderer renderer;
    @ComponentImport
    private final RepositoryService repositoryService;
    @ComponentImport
    private final ProjectService projectService;
    @ComponentImport
    private final UserCheckService userCheckService;
    @ComponentImport
    private final UserManager userManager;

    private final PluginSettings pluginSettings;

    @Inject
    public GlobalAdminServlet(
            UserManager userManager,
            LoginUriProvider loginUriProvider,
            TemplateRenderer renderer,
            RepositoryService repositoryService,
            UserCheckService userCheckService,
            PluginSettingsFactory pluginSettingsFactory,
            ProjectService projectService
    ) {
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
        this.renderer = renderer;
        this.repositoryService = repositoryService;
        this.userCheckService = userCheckService;
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
        this.projectService = projectService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            UserProfile user = this.userManager.getRemoteUser(request);
            if (user == null) {
                response.sendRedirect(this.loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
                return;
            }

            final boolean isSystemAdmin = this.userCheckService.isSystemAdmin(user.getUserKey());
            Map<String, Object> context = new HashMap<>();
            String trace = request.getParameter("trace");
            if ("y".equalsIgnoreCase(trace)) {
                if (!isSystemAdmin) {
                    response.sendError(401, "Unauthorized - only sys admins allowed here!");
                    return;
                }

                String dumpJson = request.getParameter("dumpJson");
                if ("y".equalsIgnoreCase(dumpJson)) {
                    String json = (String) pluginSettings.get(SETTINGS_STORAGE_KEY);
                    if (json != null) {
                        Object o = Java2Json.parse(json);
                        json = Java2Json.format(true, o);
                    } else {
                        json = "null";
                    }
                    response.setContentType("application/json;charset=UTF-8");
                    response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
                    return;
                }

                String refresh = request.getParameter("refresh");
                String refreshSuccess = request.getParameter("refreshSuccess");
                if ("y".equalsIgnoreCase(refresh)) {
                    HttpUtil.reset();
                    response.sendRedirect("./admin?trace=y&refreshSuccess=y");
                    return;
                } else if ("y".equalsIgnoreCase(refreshSuccess)) {
                    context.put("refreshResult", "Success");
                } else {
                    context.put("refreshResult", "");
                }

                // pop them into TreeMaps so that they are sorted by timestamp.
                context.put("successes", new TreeMap<>(HttpUtil.LAST_25_SUCCESSES).values());
                context.put("failures", new TreeMap<>(HttpUtil.LAST_25_FAILURES).values());
                context.put("errors", new TreeMap<>(HttpUtil.LAST_25_ERRORS).values());
                context.put("in_flight", new TreeMap<>(HttpUtil.LAST_25_IN_FLIGHT).values());

                context.put("activeNotifications", HttpUtil.top99_Notifications());
                context.put("activeInjections", HttpUtil.top99_Injections());
                context.put("activeButtons", HttpUtil.top99_Buttons());

                response.setContentType("text/html;charset=UTF-8");
                this.renderer.render("debug.vm", context, response.getWriter());
                return;
            }

            String projectKey = null;
            String repositorySlug = null;
            final Optional<Repository> repository = getRepository(request.getPathInfo());
            if (repository.isPresent()) {
                projectKey = repository.get().getProject().getKey();
                repositorySlug = repository.get().getSlug();
            }
            final Optional<Project> project = getProject(request.getPathInfo());
            if (project.isPresent()) {
                projectKey = project.get().getKey();
                repositorySlug = null;
            }
            boolean isAdmin = this.userCheckService.isAdmin(user.getUserKey(), projectKey, repositorySlug);

            if (repository.isPresent()) {
                context = immutableMap(
                        "repository", repository.get(), "isAdmin", isAdmin, "isSystemAdmin", isSystemAdmin
                );
            } else if (project.isPresent()) {
                context = immutableMap(
                        "project", project.get(), "isAdmin", isAdmin, "isSystemAdmin", isSystemAdmin
                );
            } else {
                context = immutableMap("isAdmin", isAdmin, "isSystemAdmin", isSystemAdmin);
            }

            response.setContentType("text/html;charset=UTF-8");
            this.renderer.render("admin.vm", context, response.getWriter());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private URI getUri(HttpServletRequest request) {
        StringBuffer builder = request.getRequestURL();
        if (request.getQueryString() != null) {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }

    public Optional<Project> getProject(String pathInfo) {
        Optional<String[]> componentsOpt = getComponents(pathInfo);
        if (!componentsOpt.isPresent() || componentsOpt.get().length != 1) {
            return empty();
        }
        String[] components = componentsOpt.get();
        String projectKey = components[0];
        Project project = projectService.getByKey(projectKey);
        return Optional.of(project);
    }

    public Optional<Repository> getRepository(String pathInfo) {
        Optional<String[]> componentsOpt = getComponents(pathInfo);
        if (!componentsOpt.isPresent() || componentsOpt.get().length != 2) {
            return empty();
        }
        String[] components = componentsOpt.get();
        String project = components[0];
        String repoSlug = components[1];
        final Repository repository = Util.checkNotNull(
                this.repositoryService.getBySlug(project, repoSlug),
                "Did not find " + project + " " + repoSlug
        );
        return Optional.of(repository);
    }

    private Optional<String[]> getComponents(String pathInfo) {
        if (pathInfo == null || pathInfo.isEmpty()) {
            return empty();
        }
        int indexOf = pathInfo.indexOf("prnfb/admin/");
        if (indexOf == -1) {
            return empty();
        }
        String root = pathInfo.substring(indexOf + "prnfb/admin/".length());
        if (root.isEmpty()) {
            return empty();
        }
        String[] split = root.split("/");
        return Optional.of(split);
    }
}
