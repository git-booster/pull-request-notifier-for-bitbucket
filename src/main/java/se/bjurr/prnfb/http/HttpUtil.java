package se.bjurr.prnfb.http;

import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.project.ProjectService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.event.api.EventListener;
import com.atlassian.plugin.event.events.PluginDisablingEvent;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import se.bjurr.prnfb.Java2Json;
import se.bjurr.prnfb.service.SettingsService;
import se.bjurr.prnfb.settings.PrnfbButton;
import se.bjurr.prnfb.settings.PrnfbNotification;
import se.bjurr.prnfb.settings.PrnfbSettings;

import javax.inject.Named;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

@ExportAsService({HttpUtil.class})
@Named("PRNotifier_HttpUtil")
public class HttpUtil implements LifecycleAware {
    private static final Logger LOG = getLogger(HttpUtil.class);
    private static volatile CloseableHttpClient main = null;
    private static final Map<HttpHost, CloseableHttpClient> proxies = new ConcurrentHashMap<>();

    public HttpUtil() {
    }

    public static final Date START_TIME = new Date();
    public static final ConcurrentHashMap<Long, String[]> LAST_25_SUCCESSES = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Long, String[]> LAST_25_FAILURES = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Long, String[]> LAST_25_ERRORS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Long, String[]> LAST_25_IN_FLIGHT = new ConcurrentHashMap<>();


    public static final ConcurrentHashMap<UUID, Integer> BUTTON_CLICK_COUNT = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Integer> NOTIFICATION_COUNT = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Integer> INJECTION_COUNT = new ConcurrentHashMap<>();

    public static void incrementButton(UUID uuid) {
        increment(BUTTON_CLICK_COUNT, uuid);
    }

    public static void incrementNotification(UUID uuid) {
        increment(NOTIFICATION_COUNT, uuid);
    }

    public static void incrementInjection(UUID uuid) {
        increment(INJECTION_COUNT, uuid);
    }

    public static List<List<String>> getObjs(
            String grep, Boolean boundOnes, ProjectService projectService, RepositoryService repositoryService,
            boolean getButtons, int page, int[] totalCount
    ) {
        grep = trimOrEmpty(grep);
        if (grep.length() < 3) {
            grep = "";
        }
        List<List<String>> list = new ArrayList<>();
        List<Struct> structs = new ArrayList<>();
        Map<String, Object> cache = new HashMap<>();
        PrnfbSettings settings = SettingsService.cachedSettings;
        if (settings != null) {
            List<Object> data = (List) (getButtons ? settings.getButtons() : settings.getNotifications());
            for (Object o : data) {
                Struct s;
                if (o instanceof PrnfbNotification) {
                    s = Struct.fromNotification(o, projectService, repositoryService, cache);
                } else {
                    s = Struct.fromButton(o, projectService, repositoryService, cache);
                }

                if (!"".equals(grep)) {
                    if (!passesGrep(grep, s)) {
                        continue; // don't add this one, no filter match.
                    }
                }
                if (boundOnes == null) {
                    structs.add(s);
                } else if (boundOnes && s.isBound) {
                    structs.add(s);
                } else if (!boundOnes && !s.isBound) {
                    structs.add(s);
                }
            }
            totalCount[0] = structs.size();
            Collections.sort(structs, BY_PROJ_REPO);
            page--;
            if (page * PAGE_SIZE >= structs.size()) {
                page = (structs.size() + 1) / PAGE_SIZE;
                page--;
            }
            page = Math.max(0, page);
            for (int i = page * PAGE_SIZE; i < PAGE_SIZE + PAGE_SIZE * page; i++) {
                if (i >= structs.size()) {
                    break;
                }
                list.add(structs.get(i).toStringList());
            }
        }
        return list;
    }

    public final static int PAGE_SIZE = 500;

    public static String insertHtmlForMatch(String raw, String needleLowerCase, String rawLowerCase) {
        raw = trimOrEmpty(raw);
        int x = rawLowerCase.indexOf(needleLowerCase);
        int len = needleLowerCase.length();
        raw = raw.substring(0, x) + "<b>" + raw.substring(x, x + len) + "</b>" + raw.substring(x + len);
        return raw;
    }

    public static boolean passesGrep(String grep, Struct s) {
        grep = trimOrEmpty(grep).toLowerCase(Locale.ROOT);
        boolean pass = false;
        if (grep.length() > 2) {
            String p = (trimOrEmpty(s.proj).toLowerCase(Locale.ROOT));
            if (p.contains(grep)) {
                pass = true;
                s.proj = insertHtmlForMatch(s.proj, grep, p);
            }
            String r = (trimOrEmpty(s.repo).toLowerCase(Locale.ROOT));
            if (r.contains(grep)) {
                pass = true;
                s.repo = insertHtmlForMatch(s.repo, grep, r);
            }
            String n = (trimOrEmpty(s.name).toLowerCase(Locale.ROOT));
            if (n.contains(grep)) {
                pass = true;
                s.name = insertHtmlForMatch(s.name, grep, n);
            }
            String u = (trimOrEmpty(s.url).toLowerCase(Locale.ROOT));
            if (u.contains(grep)) {
                pass = true;
                s.url = insertHtmlForMatch(s.url, grep, u);
            }
        }
        return pass;
    }

    public static List<List<String>> top25_Notifications() {
        return top25_Dudes(NOTIFICATION_COUNT, false, false);
    }

    public static List<List<String>> top25_Injections() {
        return top25_Dudes(INJECTION_COUNT, false, true);
    }

    public static List<List<String>> top25_Buttons() {
        return top25_Dudes(BUTTON_CLICK_COUNT, true, false);
    }

    public static List<List<String>> top25_Dudes(Map<UUID, Integer> m, boolean isButton, boolean isInjection) {
        PrnfbSettings settings = SettingsService.cachedSettings;
        List<Struct> structs = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : m.entrySet()) {
            UUID uuid = entry.getKey();
            Integer count = entry.getValue();
            if (count > 0) {
                Struct s = Struct.fromUuid(settings, uuid, count, isButton, isInjection);
                structs.add(s);
            }
        }
        Collections.sort(structs);
        List<List<String>> list = new ArrayList<>();
        for (Struct s : structs) {
            list.add(s.toStringList());
            if (list.size() >= 25) {
                break;
            }
        }
        return list;
    }

    private final static Comparator<Struct> BY_PROJ_REPO = (s1, s2) -> {
        if (s1 == s2) {
            return 0;
        } else if (s1 == null) {
            return 1;
        } else if (s2 == null) {
            return -1;
        }
        String p1 = trimOrEmpty(s1.proj);
        String p2 = trimOrEmpty(s2.proj);
        if (p1.startsWith(".disabled.")) {
            p1 = p1.substring(".disabled.".length());
        }
        if (p2.startsWith(".disabled.")) {
            p2 = p2.substring(".disabled.".length());
        }
        String r1 = trimOrEmpty(s1.repo);
        String r2 = trimOrEmpty(s2.repo);
        String n1 = trimOrEmpty(s1.name);
        String n2 = trimOrEmpty(s2.name);
        int c = p1.compareToIgnoreCase(p2);
        if (c == 0) {
            c = r1.compareToIgnoreCase(r2);
            if (c == 0) {
                c = n1.compareToIgnoreCase(n2);
                if (c == 0) {
                    c = s1.uuid.compareToIgnoreCase(s2.uuid);
                }
            }
        }
        return c;
    };

    private static class Struct implements Comparable<Struct> {
        private int count;
        private String uuidHtml;
        private String uuid;
        private String proj;
        private String repo;
        private String name;
        private String url;
        private Boolean isBound;

        public List<String> toStringList() {
            String[] s = new String[8];
            s[0] = Integer.toString(this.count);
            s[1] = trimOrEmpty(this.uuidHtml);
            s[2] = trimOrEmpty(this.proj);
            s[3] = trimOrEmpty(this.repo);
            s[4] = isBound != null ? (isBound ? "<i class='y'>Yes</b>" : "<i class='n'>No</b>") : "";
            s[5] = trimOrEmpty(this.name);
            s[6] = trimOrEmpty(this.url);
            if (s[6].length() > 99) {
                s[6] = s[6].substring(0, 99) + "...";
            }
            s[7] = trimOrEmpty(this.uuid);
            return Arrays.asList(s);
        }

        private static String htmlSafe(String s) {
            if (s != null && !"".equals(s)) {
                s = s.replace("&", "&amp;");
                s = s.replace("<", "&lt;");
                s = s.replace(">", "&gt;");
            }
            return s;
        }

        public int compareTo(Struct o) {
            return -1 * Integer.compare(count, o.count);
        }

        public void setIfBound(
                ProjectService projectService, RepositoryService repositoryService, Map<String, Object> cache
        ) {
            if (isBound != null) {
                return;
            }
            String pKey = proj != null ? proj.trim() : "";
            String rKey = repo != null ? repo.trim() : "";
            boolean hasProj = !"".equals(pKey);
            boolean hasRepo = !"".equals(rKey);

            Project p = null;
            Repository r = null;
            if (hasProj) {
                p = (Project) cache.get(pKey);
                if (p == null) {
                    try {
                        p = projectService.getByKey(proj);
                    } catch (Exception e) {
                        // swallow
                        p = null;
                    }
                    if (p != null) {
                        cache.put(pKey, p);
                    }
                }
                if (p != null) {
                    if (hasRepo) {
                        String repoCacheKey = pKey + "." + rKey;
                        r = (Repository) cache.get(repoCacheKey);
                        if (r == null) {
                            try {
                                r = repositoryService.getBySlug(pKey, rKey);
                                cache.put(repoCacheKey, r);
                            } catch (Exception e) {
                                // swallow
                                r = null;
                            }
                        }
                    }
                }
            }

            if (hasRepo && r != null) {
                isBound = true; // is an active repo-scoped notification
            } else if (!hasRepo && hasProj && p != null) {
                isBound = true; // is an active project-scoped notification
            } else if (!hasRepo && !hasProj) {
                isBound = true; // is an active global notification
            } else {
                isBound = false; // This one lost its binding (probably from a renaming)
            }
        }

        private static int count(Map<UUID, Integer> m, UUID u) {
            if (u != null) {
                Integer i = m.get(u);
                if (i != null) {
                    return i;
                }
            }
            return 0;
        }

        public static Struct fromButton(
                Object o, ProjectService projectService, RepositoryService repositoryService, Map<String, Object> cache
        ) {
            PrnfbButton b = (PrnfbButton) o;
            Struct s = new Struct();
            UUID u = b.getUuid();
            s.count = count(BUTTON_CLICK_COUNT, u);
            s.uuid = u != null ? u.toString() : "UUID=UNKNOWN";
            s.proj = b.getProjectKey().orElse("");
            s.repo = b.getRepositorySlug().orElse("");
            s.name = b.getName();
            s.url = htmlSafe(b.getRedirectUrl());
            s.setIfBound(projectService, repositoryService, cache);

            String suffix = "?myUuid=" + s.uuid;
            suffix += "#pr_buttons";
            String url = "admin";
            boolean isBound = s.isBound != null && s.isBound;
            if (isBound) {
                if (!"".equals(s.proj)) {
                    url += "/" + s.proj;
                }
                if (!"".equals(s.repo)) {
                    url += "/" + s.repo;
                }
            }
            url += suffix;
            s.uuidHtml = "<a href='" + url + "'>" + s.uuid + "</a>";
            return s;
        }

        public static Struct fromNotification(
                Object o, ProjectService projectService, RepositoryService repositoryService, Map<String, Object> cache
        ) {
            PrnfbNotification n = (PrnfbNotification) o;
            Struct s = new Struct();
            UUID u = n.getUuid();
            s.count = count(NOTIFICATION_COUNT, u);
            s.uuid = u != null ? u.toString() : "UUID=UNKNOWN";
            s.proj = n.getProjectKey().orElse("");
            s.repo = n.getRepositorySlug().orElse("");
            s.name = n.getName();
            s.url = htmlSafe(n.getUrl());
            s.setIfBound(projectService, repositoryService, cache);

            String suffix = "?myUuid=" + s.uuid;
            suffix += "#pr_notifications";
            String url = "admin";
            boolean isBound = s.isBound != null && s.isBound;
            if (isBound) {
                if (!"".equals(s.proj)) {
                    url += "/" + s.proj;
                }
                if (!"".equals(s.repo)) {
                    url += "/" + s.repo;
                }
            }
            url += suffix;
            s.uuidHtml = "<a href='" + url + "'>" + s.uuid + "</a>";
            return s;
        }

        public static Struct fromUuid(
                PrnfbSettings settings, UUID uuid, Integer count, boolean isButton, boolean isInjection
        ) {
            Struct s = new Struct();
            s.uuid = uuid.toString();
            s.count = count;
            String suffix = "?myUuid=" + uuid;

            boolean foundUuid = false;
            if (isButton) {
                suffix += "#pr_buttons";
                for (PrnfbButton b : settings.getButtons()) {
                    if (uuid.equals(b.getUuid())) {
                        foundUuid = true;
                        s.proj = b.getProjectKey().orElse("");
                        s.repo = b.getRepositorySlug().orElse("");
                        s.name = b.getName();
                        s.url = b.getRedirectUrl();
                        break;
                    }
                }
            } else {
                suffix += "#pr_notifications";
                for (PrnfbNotification n : settings.getNotifications()) {
                    if (uuid.equals(n.getUuid())) {
                        foundUuid = true;
                        s.proj = n.getProjectKey().orElse("");
                        s.repo = n.getRepositorySlug().orElse("");
                        s.name = n.getName();
                        s.url = isInjection ? n.getInjectionUrl().orElse("") : n.getUrl();
                        break;
                    }
                }
            }
            if (foundUuid) {
                String url = "admin";
                if (!"".equals(s.proj)) {
                    url += "/" + s.proj;
                }
                if (!"".equals(s.repo)) {
                    url += "/" + s.repo;
                }
                url += suffix;
                s.uuidHtml = "<a href='" + url + "'>" + uuid + "</a>";
            } else {
                s.name = "ERROR: COULD NOT FIND UUID";
            }
            s.url = htmlSafe(s.url);
            return s;
        }
    }

    public static String trimOrEmpty(String s) {
        return s != null ? s.trim() : "";
    }

    private static void increment(Map<UUID, Integer> m, UUID u) {
        if (u != null) {
            // not actually thread-safe, but we don't care if totals are off by a little
            Integer val = m.getOrDefault(u, 0);
            m.put(u, val + 1);
        }
    }

    public static void reset() {
        if (main != null) {
            try {
                main.close();
            } catch (Throwable t) {
                // swallow
            }
            main = null;
        }
        for (CloseableHttpClient c : proxies.values()) {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable t) {
                    // swallow
                }
            }
        }
        proxies.clear();
        LAST_25_SUCCESSES.clear();
        LAST_25_FAILURES.clear();
        LAST_25_ERRORS.clear();
        LAST_25_IN_FLIGHT.clear();
    }

    private static CloseableHttpClient getCachedClient(final UrlInvoker u, final HttpHost h) {
        CloseableHttpClient client;
        if (h != null) {
            // proxy=true
            client = proxies.get(h);
            if (client == null) {
                HttpClientBuilder builder = initHttpBuilder();
                configureSsl(u, builder, true);
                configureForProxy(u, h, builder);
                client = builder.build();
                proxies.put(h, client);
            }
        } else {
            // proxy=false
            client = main;
            if (client == null) {
                HttpClientBuilder builder = initHttpBuilder();
                configureSsl(u, builder, false);
                client = builder.build();
                main = client;
            }
        }
        return client;
    }

    private static HttpClientBuilder initHttpBuilder() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setConnectionManagerShared(true);
        SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).setSoTimeout(50000).build();
        builder.setDefaultSocketConfig(socketConfig);
        builder.setMaxConnTotal(900);
        builder.setMaxConnPerRoute(900);
        builder.setConnectionTimeToLive(60, TimeUnit.SECONDS);
        return builder;
    }

    public static HttpResponse doInvoke(final UrlInvoker u, final HttpRequestBase httpRequestBase) {
        SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        HttpHost h = u.getHttpHostForProxy();
        CloseableHttpClient client = getCachedClient(u, h);
        CloseableHttpResponse httpResponse = null;
        long contentLength = -1;
        if (httpRequestBase instanceof HttpEntityEnclosingRequestBase) {
            HttpEntityEnclosingRequestBase b = (HttpEntityEnclosingRequestBase) httpRequestBase;
            contentLength = b.getEntity().getContentLength();
        }
        long start = System.currentTimeMillis();
        Date d = new Date(start);
        final URI uri = httpRequestBase.getURI();
        String uriString = uri.toASCIIString();
        if (uriString.length() > 127) {
            uriString = uriString.substring(0, 127) + "...";
        }
        String[] forLog =
                new String[]{
                        df.format(d), "-", "-", httpRequestBase.getMethod(), "" + contentLength, uriString,
                        "-", "-", h != null ? "PROXY: " + h : "-"
                };
        boolean httpOkay = false;
        boolean httpKindaBad = false;
        put(LAST_25_IN_FLIGHT, start, forLog);
        long delay = -1;
        try {
            httpResponse = client.execute(httpRequestBase);
            delay = System.currentTimeMillis() - start;
            forLog[1] = delay + "ms";
            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            forLog[2] = Integer.toString(statusCode);

            final HttpEntity entity = httpResponse.getEntity();
            httpOkay = true;
            String entityString = "";
            if (entity != null) {
                entityString = EntityUtils.toString(entity, UTF_8);
            }
            forLog[6] = "" + entityString.length();

            if (200 <= statusCode && statusCode <= 299) {
                put(LAST_25_SUCCESSES, start, forLog);
            } else {
                httpKindaBad = true;
                put(LAST_25_FAILURES, start, forLog);
            }
            return new HttpResponse(uri, statusCode, entityString);

        } catch (final Exception e) {
            httpOkay = false;
            if (delay == -1) {
                delay = System.currentTimeMillis() - start;
            }
            forLog[1] = delay + "ms";
            forLog[2] = "ERR";
            forLog[8] = e.toString();

            put(LAST_25_ERRORS, start, forLog);
            throw new RuntimeException(e);
        } finally {
            if (httpOkay) {
                if (httpKindaBad) {
                    LOG.warn("PR-Notifier-HTTP-BAD - " + Java2Json.format(forLog));
                } else {
                    LOG.info("PR-Notifier-HTTP-OK  - " + Java2Json.format(forLog));
                }
            } else {
                LOG.error("PR-Notifier-HTTP-ERR - " + Java2Json.format(forLog));
            }
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void put(final Map<Long, String[]> m, final Long l, final String[] v) {

        // Make sure Map not too big...
        TreeMap<Long, String[]> tm = new TreeMap<>(m);
        long toDelete = tm.size() - 24;
        if (toDelete > 0) {
            for (Long k : tm.keySet()) {
                m.remove(k);
                toDelete--;
                if (toDelete <= 0) {
                    break;
                }
            }
        }

        // Actually put in our guy...
        m.put(l, v);

        // Remove from "IN_FLIGHT" map if appropriate.
        if (m != LAST_25_IN_FLIGHT) {
            LAST_25_IN_FLIGHT.remove(l);
        }

    }

    private static SSLContext newSslContext(UrlInvoker u) throws Exception {
        SSLContextBuilder sslContextBuilder = SSLContexts.custom();
        if (u.shouldAcceptAnyCertificate()) {
            sslContextBuilder = doAcceptAnyCertificate(sslContextBuilder);
        }
        ClientKeyStore cks = u.getClientKeyStore();
        if (cks != null && cks.getKeyStore().isPresent()) {
            sslContextBuilder.loadKeyMaterial(cks.getKeyStore().get(), cks.getPassword());
        }
        return sslContextBuilder.build();
    }

    private static SSLContextBuilder doAcceptAnyCertificate(SSLContextBuilder customContext)
            throws Exception {
        final TrustStrategy easyStrategy =
                new TrustStrategy() {
                    @Override
                    public boolean isTrusted(final X509Certificate[] chain, final String authType) {
                        return true;
                    }
                };
        customContext = customContext.loadTrustMaterial(null, easyStrategy);
        return customContext;
    }

    private static void configureSsl(
            UrlInvoker u, final HttpClientBuilder builder, final boolean isProxy) {
        PoolingHttpClientConnectionManager cm = null;
        try {
            SSLContext s = newSslContext(u);
            SSLConnectionSocketFactory sslConnSocketFactory = new SSLConnectionSocketFactory(s);
            builder.setSSLSocketFactory(sslConnSocketFactory);
            builder.setSSLContext(s);
            Registry<ConnectionSocketFactory> registry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("https", sslConnSocketFactory)
                            .register("http", PlainConnectionSocketFactory.getSocketFactory())
                            .build();
            cm = new PoolingHttpClientConnectionManager(registry);
        } catch (final Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }

        final RequestConfig config = RequestConfig.custom()
                .setMaxRedirects(8)
                .setConnectTimeout(7500)
                .setConnectionRequestTimeout(7500)
                .setSocketTimeout(45000)
                .build();
        builder.setDefaultRequestConfig(config);
        if (isProxy) {
            cm.setMaxTotal(12);
            cm.setDefaultMaxPerRoute(4);
        } else {
            cm.setMaxTotal(42);
            cm.setDefaultMaxPerRoute(8);
        }
        builder.setConnectionManager(cm);
    }

    public static void configureForProxy(
            final UrlInvoker u, final HttpHost h, final HttpClientBuilder builder) {
        if (u.getProxyUser().isPresent() && u.getProxyPassword().isPresent()) {
            final String username = u.getProxyUser().get();
            final String password = u.getProxyPassword().get();
            final UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
            final CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(h.getHostName(), h.getPort()), creds);
            builder.setDefaultCredentialsProvider(credsProvider);
        }
        builder.useSystemProperties();
        builder.setProxy(h);
        builder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
        reset();
    }

    // This is the important one (onPluginDisabling) that actually gets invoked on shutdown!
    @EventListener
    public void onPluginDisabling(final PluginDisablingEvent event) {
        onStop();
    }
}
