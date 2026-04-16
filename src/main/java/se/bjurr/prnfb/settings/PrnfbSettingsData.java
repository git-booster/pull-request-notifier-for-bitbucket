package se.bjurr.prnfb.settings;

import se.bjurr.prnfb.Java2Json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static se.bjurr.prnfb.Util.emptyToNull;

public class PrnfbSettingsData implements Java2Json._2JS {
    private USER_LEVEL adminRestriction;
    private String keyStore;
    private String keyStorePassword;
    private String keyStoreType;
    private boolean shouldAcceptAnyCertificate;

    public PrnfbSettingsData() {
    }

    public Map<String, Object> _2js() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("adminRestriction", this.adminRestriction);
        m.put("keyStore", this.keyStore);
        m.put("keyStorePassword", this.keyStorePassword);
        m.put("keyStoreType", this.keyStoreType);
        m.put("shouldAcceptAnyCertificate", this.shouldAcceptAnyCertificate);
        return m;
    }

    public static PrnfbSettingsData _fjs(Map<String, Object> m) {
        PrnfbSettingsData p = new PrnfbSettingsData();
        Boolean b = (Boolean) m.get("shouldAcceptAnyCertificate");
        p.adminRestriction = USER_LEVEL.fromObject(m.get("adminRestriction"));
        p.keyStore = (String) m.get("keyStore");
        p.keyStorePassword = (String) m.get("keyStorePassword");
        p.keyStoreType = (String) m.get("keyStoreType");
        p.shouldAcceptAnyCertificate = b != null ? b : false;
        return p;
    }

    public PrnfbSettingsData(PrnfbSettingsDataBuilder builder) {
        this.keyStore = emptyToNull(builder.getKeyStore());
        this.keyStoreType = builder.getKeyStoreType();
        this.keyStorePassword = emptyToNull(builder.getKeyStorePassword());
        this.shouldAcceptAnyCertificate = builder.shouldAcceptAnyCertificate();
        this.adminRestriction = builder.getAdminRestriction();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PrnfbSettingsData other = (PrnfbSettingsData) obj;
        if (this.adminRestriction != other.adminRestriction) {
            return false;
        }
        if (this.keyStore == null) {
            if (other.keyStore != null) {
                return false;
            }
        } else if (!this.keyStore.equals(other.keyStore)) {
            return false;
        }
        if (this.keyStorePassword == null) {
            if (other.keyStorePassword != null) {
                return false;
            }
        } else if (!this.keyStorePassword.equals(other.keyStorePassword)) {
            return false;
        }
        if (this.keyStoreType == null) {
            if (other.keyStoreType != null) {
                return false;
            }
        } else if (!this.keyStoreType.equals(other.keyStoreType)) {
            return false;
        }
        if (this.shouldAcceptAnyCertificate != other.shouldAcceptAnyCertificate) {
            return false;
        }
        return true;
    }

    public USER_LEVEL getAdminRestriction() {
        return this.adminRestriction;
    }

    public Optional<String> getKeyStore() {
        return ofNullable(this.keyStore);
    }

    public Optional<String> getKeyStorePassword() {
        return ofNullable(this.keyStorePassword);
    }

    public String getKeyStoreType() {
        return this.keyStoreType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result =
                prime * result + ((this.adminRestriction == null) ? 0 : this.adminRestriction.hashCode());
        result = prime * result + ((this.keyStore == null) ? 0 : this.keyStore.hashCode());
        result =
                prime * result + ((this.keyStorePassword == null) ? 0 : this.keyStorePassword.hashCode());
        result = prime * result + ((this.keyStoreType == null) ? 0 : this.keyStoreType.hashCode());
        result = prime * result + (this.shouldAcceptAnyCertificate ? 1231 : 1237);
        return result;
    }

    public boolean isShouldAcceptAnyCertificate() {
        return this.shouldAcceptAnyCertificate;
    }

    @Override
    public String toString() {
        return "PrnfbSettingsData [keyStore="
                + this.keyStore
                + ", keyStoreType="
                + this.keyStoreType
                + ", keyStorePassword="
                + this.keyStorePassword
                + ", shouldAcceptAnyCertificate="
                + this.shouldAcceptAnyCertificate
                + ", adminRestriction="
                + this.adminRestriction
                + "]";
    }
}
