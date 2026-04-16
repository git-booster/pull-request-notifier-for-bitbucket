package se.bjurr.prnfb.settings;

import se.bjurr.prnfb.Java2Json;

import java.util.LinkedHashMap;
import java.util.Map;

public class PrnfbHeader implements Java2Json._2JS {

    private String name;
    private String value;

    public PrnfbHeader() {
    }

    public Map<String, Object> _2js() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", this.name);
        m.put("value", this.value);
        return name != null && !"".equals(name) ? m : null;
    }

    public static PrnfbHeader _fjs(Map<String, Object> m) {
        if (m != null) {
            String name = (String) m.get("name");
            name = name != null ? name.trim() : "";
            String value = (String) m.get("value");
            if (!"".equals(name)) {
                PrnfbHeader h = new PrnfbHeader();
                h.name = name;
                h.value = value;
                return h;
            }
        }
        return null;
    }

    public PrnfbHeader(String name, String value) {
        this.name = name != null ? name.trim() : "";
        this.value = value != null ? value.trim() : "";
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
        PrnfbHeader other = (PrnfbHeader) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!this.value.equals(other.value)) {
            return false;
        }
        return true;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public void setName(String s) {
        this.name = s;
    }

    public void setValue(String s) {
        this.value = s;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + (this.value == null ? 0 : this.value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "PrnfbHeader [name=" + this.name + ", value=" + this.value + "]";
    }
}
