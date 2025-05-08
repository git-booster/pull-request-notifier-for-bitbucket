package se.bjurr.prnfb.http;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Optional;
import org.slf4j.Logger;
import se.bjurr.prnfb.settings.PrnfbSettingsData;

/**
 * A keystore based on the definition from the application properties.<br>
 * <br>
 * Inspired by:<br>
 * Philip Dodds (pdodds) https://github.com/pdodds
 */
public class ClientKeyStore {
  private static final Logger LOG = getLogger(ClientKeyStore.class);

  private KeyStore keyStore = null;
  private char[] password = null;

  public ClientKeyStore(PrnfbSettingsData settings) {
    if (settings.getKeyStore().isPresent()) {
      File keyStoreFile = new File(settings.getKeyStore().get());
      try {
        this.keyStore = getKeyStore(settings.getKeyStoreType());

        if (settings.getKeyStorePassword().isPresent()) {
          this.password = settings.getKeyStorePassword().get().toCharArray();
        }

        this.keyStore.load(new FileInputStream(keyStoreFile), this.password);
      } catch (Exception e) {
        String msg = "Failed to load keystore [" + settings.getKeyStore().orElse("undefined") + "]";
        String type = " Type=" + settings.getKeyStoreType();
        LOG.warn("Pull-Request-Notifier-For-Bitbucket - " + msg + type + " - " + e, e);
      }
    }
  }

  public Optional<KeyStore> getKeyStore() {
    return ofNullable(this.keyStore);
  }

  public char[] getPassword() {
    return this.password;
  }

  private KeyStore getKeyStore(String keyStoreType) throws KeyStoreException {
    if (keyStoreType != null) {
      return KeyStore.getInstance(keyStoreType);
    } else {
      return KeyStore.getInstance(KeyStore.getDefaultType());
    }
  }
}
