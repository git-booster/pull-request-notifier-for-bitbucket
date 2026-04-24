package se.bjurr.prnfb.presentation.dto;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import se.bjurr.prnfb.Java2Json;

import java.util.LinkedHashMap;
import java.util.Map;

import static javax.xml.bind.annotation.XmlAccessType.FIELD;

/**
 * @see ButtonFormElementDTO
 */
@XmlRootElement
@XmlAccessorType(FIELD)
public class ButtonFormElementOptionDTO implements Java2Json._2JS {
    private String label;
    private String name;
    private Boolean defaultValue;

    public Map<String, Object> _2js() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("name", name);
        m.put("defaultValue", defaultValue);
        return m;
    }

    public static ButtonFormElementOptionDTO _fjs(Map<String, Object> m) {
        ButtonFormElementOptionDTO dto = new ButtonFormElementOptionDTO();
        Boolean b = (Boolean) m.get("defaultValue");
        dto.label = (String) m.get("label");
        dto.name = (String) m.get("name");
        dto.defaultValue = b != null ? b : false;
        return dto.name != null ? dto : null;
    }

    public void setDefaultValue(Boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDefaultValue() {
        return defaultValue;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ButtonFormOptionDTO [label="
                + label
                + ", name="
                + name
                + ", defaultValue="
                + defaultValue
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (defaultValue == null ? 0 : defaultValue.hashCode());
        result = prime * result + (label == null ? 0 : label.hashCode());
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
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
        ButtonFormElementOptionDTO other = (ButtonFormElementOptionDTO) obj;
        if (defaultValue == null) {
            if (other.defaultValue != null) {
                return false;
            }
        } else if (!defaultValue.equals(other.defaultValue)) {
            return false;
        }
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!label.equals(other.label)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
}
