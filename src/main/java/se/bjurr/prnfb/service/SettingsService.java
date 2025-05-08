package se.bjurr.prnfb.service;

import static com.atlassian.bitbucket.permission.Permission.ADMIN;
import static se.bjurr.prnfb.Util.findUuidMatch;
import static se.bjurr.prnfb.Util.newListWithoutUuid;
import static se.bjurr.prnfb.settings.PrnfbNotificationBuilder.prnfbNotificationBuilder;
import static se.bjurr.prnfb.settings.PrnfbSettings.UNCHANGED;
import static se.bjurr.prnfb.settings.PrnfbSettingsBuilder.prnfbSettingsBuilder;
import static se.bjurr.prnfb.settings.PrnfbSettingsDataBuilder.prnfbSettingsDataBuilder;

import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.Operation;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.bjurr.prnfb.Util;
import se.bjurr.prnfb.http.HttpUtil;
import se.bjurr.prnfb.settings.PrnfbButton;
import se.bjurr.prnfb.settings.PrnfbNotification;
import se.bjurr.prnfb.settings.PrnfbSettings;
import se.bjurr.prnfb.settings.PrnfbSettingsData;
import se.bjurr.prnfb.settings.USER_LEVEL;
import se.bjurr.prnfb.settings.ValidationException;

public class SettingsService {

  public static final String SETTINGS_STORAGE_KEY =
      "se.bjurr.prnfb.pull-request-notifier-for-bitbucket-3";
  private static ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.registerModule(new Jdk8Module());
  }

  private final Logger logger = LoggerFactory.getLogger(SettingsService.class);
  private final PluginSettings pluginSettings;
  private final SecurityService securityService;
  private final TransactionTemplate transactionTemplate;

  private static final Object lock = new Object();
  static volatile PrnfbSettings cachedSettings = null;

  static volatile PrnfbSettingsData lastSeenGlobalSettings = null;

  static volatile long nextCacheExpiry = 0;

  public SettingsService(
      PluginSettingsFactory pluginSettingsFactory,
      TransactionTemplate transactionTemplate,
      SecurityService securityService) {
    this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
    this.transactionTemplate = transactionTemplate;
    this.securityService = securityService;
  }

  public PrnfbButton addOrUpdateButton(PrnfbButton prnfbButton) {
    return inSynchronizedTransaction(
        new TransactionCallback<PrnfbButton>() {
          @Override
          public PrnfbButton doInTransaction() {
            return doAddOrUpdateButton(prnfbButton);
          }
        });
  }

  public PrnfbNotification addOrUpdateNotification(PrnfbNotification prnfbNotification)
      throws ValidationException {
    return inSynchronizedTransaction(
        new TransactionCallback<PrnfbNotification>() {
          @Override
          public PrnfbNotification doInTransaction() {
            try {
              return doAddOrUpdateNotification(prnfbNotification);
            } catch (final ValidationException e) {
              throw new RuntimeException(e);
            }
          }
        });
  }

  public void deleteButton(UUID uuid) {
    inSynchronizedTransaction(
        new TransactionCallback<Void>() {
          @Override
          public Void doInTransaction() {
            doDeleteButton(uuid);
            return null;
          }
        });
  }

  public void deleteNotification(UUID uuid) {
    inSynchronizedTransaction(
        new TransactionCallback<Void>() {
          @Override
          public Void doInTransaction() {
            doDeleteNotification(uuid);
            return null;
          }
        });
  }

  public Optional<PrnfbButton> findButton(UUID uuid) {
    return findUuidMatch(getPrnfbSettings().getButtons(), uuid);
  }

  public Optional<PrnfbNotification> findNotification(UUID notificationUuid) {
    return findUuidMatch(getPrnfbSettings().getNotifications(), notificationUuid);
  }

  public PrnfbButton getButton(UUID buttionUuid) {
    final Optional<PrnfbButton> foundOpt = findButton(buttionUuid);
    if (!foundOpt.isPresent()) {
      throw new RuntimeException(
          buttionUuid + " not fond in:\n" + Util.listToString("\n", getButtons()));
    }
    return foundOpt.get();
  }

  public List<PrnfbButton> getButtons() {
    return getPrnfbSettings().getButtons();
  }

  public List<PrnfbButton> getButtons(Project p) {
    String projectKey = p.getKey();
    final List<PrnfbButton> found = new ArrayList<>();
    for (final PrnfbButton candidate : getPrnfbSettings().getButtons()) {
      if (candidate.getProjectKey().isPresent()
          && candidate.getProjectKey().get().equals(projectKey)) {
        found.add(candidate);
      }
    }
    return found;
  }

  public List<PrnfbButton> getButtons(Repository r) {
    String projectKey = r.getProject().getKey();
    String repositorySlug = r.getSlug();

    final List<PrnfbButton> found = new ArrayList<>();
    for (final PrnfbButton candidate : getPrnfbSettings().getButtons()) {
      if (candidate.getProjectKey().isPresent()
          && candidate.getProjectKey().get().equals(projectKey) //
          && candidate.getRepositorySlug().isPresent()
          && candidate.getRepositorySlug().get().equals(repositorySlug)) {
        found.add(candidate);
      }
    }
    return found;
  }

  public PrnfbNotification getNotification(UUID notificationUuid) {
    return findUuidMatch(getPrnfbSettings().getNotifications(), notificationUuid).orElse(null);
  }

  public List<PrnfbNotification> getNotifications() {
    return getPrnfbSettings().getNotifications();
  }

  public List<PrnfbNotification> getNotifications(String projectKey) {
    final List<PrnfbNotification> found = new ArrayList<>();
    for (final PrnfbNotification candidate : getPrnfbSettings().getNotifications()) {
      if (candidate.getProjectKey().isPresent()
          && candidate.getProjectKey().get().equals(projectKey)) {
        found.add(candidate);
      }
    }
    return found;
  }

  public List<PrnfbNotification> getNotifications(String projectKey, String repositorySlug) {
    final List<PrnfbNotification> found = new ArrayList<>();
    for (final PrnfbNotification candidate : getPrnfbSettings().getNotifications()) {
      if (candidate.getProjectKey().isPresent()
          && candidate.getProjectKey().get().equals(projectKey) //
          && candidate.getRepositorySlug().isPresent()
          && candidate.getRepositorySlug().get().equals(repositorySlug)) {
        found.add(candidate);
      }
    }
    return found;
  }

  public PrnfbSettings getPrnfbSettings() {
    return doGetPrnfbSettings(false);
  }

  public PrnfbSettingsData getPrnfbSettingsData() {
    return getPrnfbSettings().getPrnfbSettingsData();
  }

  public void setPrnfbSettingsData(PrnfbSettingsData prnfbSettingsData) {
    inSynchronizedTransaction(
        new TransactionCallback<Void>() {
          @Override
          public Void doInTransaction() {
            final PrnfbSettings oldSettings = doGetPrnfbSettings(true);
            final PrnfbSettings newPrnfbSettings =
                prnfbSettingsBuilder(oldSettings) //
                    .setPrnfbSettingsData(prnfbSettingsData) //
                    .build();
            doSetPrnfbSettings(newPrnfbSettings);
            return null;
          }
        });
  }

  private PrnfbButton doAddOrUpdateButton(PrnfbButton prnfbButton) {
    if (findButton(prnfbButton.getUuid()).isPresent()) {
      doDeleteButton(prnfbButton.getUuid());
    }

    final PrnfbSettings originalSettings = doGetPrnfbSettings(true);
    final PrnfbSettings updated =
        prnfbSettingsBuilder(originalSettings) //
            .withButton(prnfbButton) //
            .build();

    doSetPrnfbSettings(updated);
    return prnfbButton;
  }

  private PrnfbNotification doAddOrUpdateNotification(PrnfbNotification newNotification)
      throws ValidationException {
    final UUID notificationUuid = newNotification.getUuid();

    Optional<String> oldUser = Optional.empty();
    Optional<String> oldPassword = Optional.empty();
    Optional<String> oldProxyUser = Optional.empty();
    Optional<String> oldProxyPassword = Optional.empty();
    final Optional<PrnfbNotification> oldNotification = findNotification(notificationUuid);
    if (oldNotification.isPresent()) {
      oldUser = oldNotification.get().getUser();
      oldPassword = oldNotification.get().getPassword();
      oldProxyUser = oldNotification.get().getProxyUser();
      oldProxyPassword = oldNotification.get().getProxyPassword();
    }

    final String user = keepIfUnchanged(newNotification.getUser(), oldUser);
    final String password = keepIfUnchanged(newNotification.getPassword(), oldPassword);
    final String proxyUser = keepIfUnchanged(newNotification.getProxyUser(), oldProxyUser);
    final String proxyPassword =
        keepIfUnchanged(newNotification.getProxyPassword(), oldProxyPassword);
    newNotification =
        prnfbNotificationBuilder(newNotification) //
            .withUser(user) //
            .withPassword(password) //
            .withProxyUser(proxyUser) //
            .withProxyPassword(proxyPassword) //
            .build();

    if (oldNotification.isPresent()) {
      doDeleteNotification(notificationUuid);
    }

    final PrnfbSettings originalSettings = doGetPrnfbSettings(true);
    final PrnfbSettings updated =
        prnfbSettingsBuilder(originalSettings) //
            .withNotification(newNotification) //
            .build();

    doSetPrnfbSettings(updated);
    return newNotification;
  }

  private String keepIfUnchanged(Optional<String> newValue, Optional<String> oldValue) {
    final boolean isUnchanged = newValue.isPresent() && newValue.get().equals(UNCHANGED);
    if (isUnchanged) {
      return oldValue.orElse(null);
    }
    return newValue.orElse(null);
  }

  private void doDeleteButton(UUID uuid) {
    final PrnfbSettings originalSettings = doGetPrnfbSettings(true);
    final List<PrnfbButton> keep = newListWithoutUuid(originalSettings.getButtons(), uuid);
    final PrnfbSettings withoutDeleted =
        prnfbSettingsBuilder(originalSettings) //
            .setButtons(keep) //
            .build();
    doSetPrnfbSettings(withoutDeleted);
  }

  private void doDeleteNotification(UUID uuid) {
    final PrnfbSettings originalSettings = doGetPrnfbSettings(true);
    final List<PrnfbNotification> keep =
        newListWithoutUuid(originalSettings.getNotifications(), uuid);
    final PrnfbSettings withoutDeleted =
        prnfbSettingsBuilder(originalSettings) //
            .setNotifications(keep) //
            .build();
    doSetPrnfbSettings(withoutDeleted);
  }

  private PrnfbSettings doGetPrnfbSettings(boolean forceRead) {
    long now = System.currentTimeMillis();
    if (now >= nextCacheExpiry || forceRead) {
      synchronized (lock) {
        if (now >= nextCacheExpiry || forceRead) {

          // Cache expired... re-read value from database and re-cache it
          String s = (String) this.pluginSettings.get(SETTINGS_STORAGE_KEY);

          if (s != null) {
            // Successfully retrieved a real value... use it as our cache for next 33 seconds!
            nextCacheExpiry = System.currentTimeMillis() + 33333L;

            try {
              cachedSettings = objectMapper.readValue(s, PrnfbSettings.class);
            } catch (Exception e) {
              throw new RuntimeException(
                  "failed to deserialize JSON into PrnfbSettings object: " + e, e);
            }

            // If the keystore or "accept-all-certificates" value changed, we need
            // to reset HttpUtil's connection-managers.
            PrnfbSettingsData latestData = cachedSettings.getPrnfbSettingsData();
            if (latestData != null && !latestData.equals(lastSeenGlobalSettings)) {
              HttpUtil.reset();
              lastSeenGlobalSettings = latestData;
            }

          } else {
            cachedSettings = null;
          }
        }
      }
    }

    if (cachedSettings == null) {
      // Empty initialization case (rare):  don't bother setting any cache.
      this.logger.info("Creating new default settings.");
      return prnfbSettingsBuilder() //
          .setPrnfbSettingsData( //
              prnfbSettingsDataBuilder() //
                  .setAdminRestriction(USER_LEVEL.ADMIN) //
                  .build()) //
          .build();
    } else {
      return cachedSettings;
    }
  }

  private void doSetPrnfbSettings(PrnfbSettings newSettings) {
    final PrnfbSettingsData oldSettingsData = doGetPrnfbSettings(false).getPrnfbSettingsData();
    final PrnfbSettingsData newSettingsData = newSettings.getPrnfbSettingsData();
    final String keyStorePassword =
        keepIfUnchanged(
            newSettingsData.getKeyStorePassword(), oldSettingsData.getKeyStorePassword());

    final PrnfbSettingsData adjustedSettingsData =
        prnfbSettingsDataBuilder(newSettingsData) //
            .setKeyStorePassword(keyStorePassword) //
            .build();

    final PrnfbSettings adjustedSettings =
        prnfbSettingsBuilder(newSettings) //
            .setPrnfbSettingsData(adjustedSettingsData) //
            .build();

    try {
      final String data = objectMapper.writeValueAsString(adjustedSettings);
      final PrnfbSettings adjustedSettingsReparsed =
          objectMapper.readValue(data, PrnfbSettings.class);
      this.pluginSettings.put(SETTINGS_STORAGE_KEY, data);
      cachedSettings = adjustedSettingsReparsed;
    } catch (Exception e) {
      throw new RuntimeException("failed to reparse JSON into PrnfbSettings object: " + e, e);
    }
  }

  private synchronized <T> T inSynchronizedTransaction(TransactionCallback<T> transactionCallback) {
    return this.securityService //
        .withPermission(ADMIN, "Getting config") //
        .call(
            new Operation<T, RuntimeException>() {
              @Override
              public T perform() throws RuntimeException {
                return SettingsService.this.transactionTemplate.execute(transactionCallback);
              }
            });
  }
}
