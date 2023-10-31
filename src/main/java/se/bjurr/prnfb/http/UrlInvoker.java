package se.bjurr.prnfb.http;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.apache.http.HttpVersion.HTTP_1_0;
import static org.apache.http.HttpVersion.HTTP_1_1;
import static org.slf4j.LoggerFactory.getLogger;
import static se.bjurr.prnfb.http.UrlInvoker.HTTP_METHOD.GET;
import static se.bjurr.prnfb.http.UrlInvoker.HTTP_METHOD.POST;
import static se.bjurr.prnfb.http.UrlInvoker.HTTP_METHOD.PUT;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import se.bjurr.prnfb.settings.PrnfbHeader;
import se.bjurr.prnfb.settings.PrnfbNotification;

/**
 * If told to accept all certificates, an unsafe X509 trust manager is used.<br>
 * <br>
 * If setup of the "trust-all" HttpClient fails, a non-configured HttpClient is returned.<br>
 * <br>
 * Inspired by:<br>
 * Philip Dodds (pdodds) https://github.com/pdodds<br>
 * Michael Irwin (mikesir87) https://github.com/Nerdwin15<br>
 */
public class UrlInvoker {

  public enum HTTP_METHOD {
    DELETE,
    GET,
    POST,
    PUT
  }

  private static final Logger LOG = getLogger(UrlInvoker.class);

  @VisibleForTesting
  public static String getHeaderValue(final PrnfbHeader header) {
    return header.getValue();
  }

  public static UrlInvoker urlInvoker() {
    return new UrlInvoker();
  }

  private ClientKeyStore clientKeyStore;
  private final List<PrnfbHeader> headers = newArrayList();
  private HTTP_METHOD method = GET;
  private Optional<String> postContent = absent();
  private Optional<String> proxyHost = absent();
  private Optional<String> proxyPassword = absent();
  private Optional<Integer> proxyPort = absent();
  private Optional<String> proxySchema = absent();
  private Optional<String> proxyUser = absent();
  private HttpResponse response;

  private boolean shouldAcceptAnyCertificate;

  private String urlParam;
  private ProtocolVersion httpVersion = HttpVersion.HTTP_1_0;

  UrlInvoker() {}

  public UrlInvoker appendBasicAuth(final PrnfbNotification notification) {
    if (notification.getUser().isPresent() && notification.getPassword().isPresent()) {
      final String userpass = notification.getUser().get() + ":" + notification.getPassword().get();
      final String basicAuth = "Basic " + new String(printBase64Binary(userpass.getBytes(UTF_8)));
      withHeader(AUTHORIZATION, basicAuth);
    }
    return this;
  }

  public ClientKeyStore getClientKeyStore() {
    return this.clientKeyStore;
  }

  public List<PrnfbHeader> getHeaders() {
    return this.headers;
  }

  public HTTP_METHOD getMethod() {
    return this.method;
  }

  public Optional<String> getPostContent() {
    return this.postContent;
  }

  public Optional<String> getProxyHost() {
    return this.proxyHost;
  }

  public Optional<String> getProxyPassword() {
    return this.proxyPassword;
  }

  public Optional<Integer> getProxyPort() {
    return this.proxyPort;
  }

  public Optional<String> getProxySchema() {
    return proxySchema;
  }

  public Optional<String> getProxyUser() {
    return this.proxyUser;
  }

  public HttpResponse getResponse() {
    return this.response;
  }

  public InputStream getResponseStringStream() {
    return new ByteArrayInputStream(getResponse().getContent().getBytes(UTF_8));
  }

  public String getUrlParam() {
    return this.urlParam;
  }

  public UrlInvoker setHttpVersion(final String httpVersion) {
    if (httpVersion == null || httpVersion.equals("HTTP_1_0")) {
      this.httpVersion = HTTP_1_0;
    } else if (httpVersion.equals("HTTP_1_1")) {
      this.httpVersion = HTTP_1_1;
    } else {
      this.httpVersion = HTTP_1_0;
    }
    return this;
  }

  public HttpResponse invoke() {
    LOG.info("Url: \"" + this.urlParam + "\"");

    final HttpRequestBase httpRequestBase = newHttpRequestBase();
    configureUrl(httpRequestBase);
    addHeaders(httpRequestBase);
    httpRequestBase.setProtocolVersion(httpVersion);

    this.response = HttpUtil.doInvoke(this, httpRequestBase);
    if (LOG.isDebugEnabled()) {
      if (this.response != null) {
        LOG.debug(this.response.getContent());
      }
    }
    return this.response;
  }

  public void setResponse(final HttpResponse response) {
    this.response = response;
  }

  @VisibleForTesting
  public boolean shouldAcceptAnyCertificate() {
    return this.shouldAcceptAnyCertificate;
  }

  public UrlInvoker shouldAcceptAnyCertificate(final boolean shouldAcceptAnyCertificate) {
    this.shouldAcceptAnyCertificate = shouldAcceptAnyCertificate;
    return this;
  }

  @VisibleForTesting
  public boolean shouldAuthenticateProxy() {
    return getProxyUser().isPresent() && getProxyPassword().isPresent();
  }

  @VisibleForTesting
  public boolean shouldPostContent() {
    return (this.method == POST || this.method == PUT) && this.postContent.isPresent();
  }

  private boolean shouldUseProxy() {
    return getProxyHost().isPresent() && getProxyPort().isPresent() && getProxyPort().get() > 0;
  }

  public HttpHost getHttpHostForProxy() {
    if (shouldUseProxy()) {
      return new HttpHost(this.proxyHost.get(), this.proxyPort.get(), this.proxySchema.orNull());
    } else {
      return null;
    }
  }

  public UrlInvoker withClientKeyStore(final ClientKeyStore clientKeyStore) {
    this.clientKeyStore = clientKeyStore;
    return this;
  }

  public UrlInvoker withHeader(final String name, final String value) {
    this.headers.add(new PrnfbHeader(name, value));
    return this;
  }

  public UrlInvoker withMethod(final HTTP_METHOD method) {
    this.method = method;
    return this;
  }

  public UrlInvoker withPostContent(final Optional<String> postContent) {
    this.postContent = postContent;
    return this;
  }

  public UrlInvoker withProxyPassword(final Optional<String> proxyPassword) {
    this.proxyPassword = proxyPassword;
    return this;
  }

  public UrlInvoker withProxyPort(final Integer proxyPort) {
    this.proxyPort = fromNullable(proxyPort);
    return this;
  }

  public UrlInvoker withProxySchema(final Optional<String> proxySchema) {
    this.proxySchema = proxySchema;
    return this;
  }

  public UrlInvoker withProxyServer(final Optional<String> proxyHost) {
    this.proxyHost = proxyHost;
    return this;
  }

  public UrlInvoker withProxyUser(final Optional<String> proxyUser) {
    this.proxyUser = proxyUser;
    return this;
  }

  public UrlInvoker withUrlParam(final String urlParam) {
    this.urlParam = urlParam.replaceAll("\\s", "%20");
    return this;
  }

  void addHeaders(final HttpRequestBase httpRequestBase) {
    for (final PrnfbHeader header : this.headers) {

      if (header.getName().equals(AUTHORIZATION)) {
        LOG.debug("header: \"" + header.getName() + "\" value: \"**********\"");
      } else {
        LOG.debug("header: \"" + header.getName() + "\" value: \"" + header.getValue() + "\"");
      }

      httpRequestBase.addHeader(header.getName(), getHeaderValue(header));
    }
  }

  void configureUrl(final HttpRequestBase httpRequestBase) {
    try {
      httpRequestBase.setURI(new URI(this.urlParam));
    } catch (final URISyntaxException e) {
      propagate(e);
    }
  }

  boolean shouldUseSsl() {
    return this.urlParam.startsWith("https");
  }

  @VisibleForTesting
  HttpEntityEnclosingRequestBase newHttpEntityEnclosingRequestBase(
      final HTTP_METHOD method, final String entity) {
    final HttpEntityEnclosingRequestBase entityEnclosing =
        new HttpEntityEnclosingRequestBase() {
          @Override
          public String getMethod() {
            return method.name();
          }
        };
    if (entity != null) {
      entityEnclosing.setEntity(new ByteArrayEntity(entity.getBytes()));
    }
    return entityEnclosing;
  }

  @VisibleForTesting
  HttpRequestBase newHttpRequestBase() {
    if (shouldPostContent()) {
      return newHttpEntityEnclosingRequestBase(this.method, this.postContent.get());
    }
    return newHttpRequestBase(this.method);
  }

  @VisibleForTesting
  HttpRequestBase newHttpRequestBase(final HTTP_METHOD method) {
    return new HttpRequestBase() {
      @Override
      public String getMethod() {
        return method.name();
      }
    };
  }
}
