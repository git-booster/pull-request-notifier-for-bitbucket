package se.bjurr.prnfb.presentation.dto;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import se.bjurr.prnfb.Java2Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static javax.xml.bind.annotation.XmlAccessType.FIELD;

@XmlRootElement
@XmlAccessorType(FIELD)
public class ButtonFormElementDTO implements Java2Json._2JS {
    private String defaultValue;
    private String description;
    private String label;
    private String name;
    private List<ButtonFormElementOptionDTO> buttonFormElementOptionList;
    private boolean required;
    private ButtonFormType type;

    public Map<String, Object> _2js() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("defaultValue", defaultValue);
        m.put("description", description);
        m.put("label", label);
        m.put("name", name);
        m.put("buttonFormElementOptionList", Java2Json._2List((List) this.buttonFormElementOptionList));
        m.put("required", required);
        m.put("type", type);
        return m;
    }

    public static ButtonFormElementDTO _fjs(Map<String, Object> m) {
        ButtonFormElementDTO dto = new ButtonFormElementDTO();
        Boolean b = (Boolean) m.get("required");
        List<Map<String, Object>> list = (List) m.get("buttonFormElementOptionList");
        List<ButtonFormElementOptionDTO> options = new ArrayList<>();
        for (Map<String, Object> mm : list) {
            ButtonFormElementOptionDTO option = ButtonFormElementOptionDTO._fjs(mm);
            if (option != null) {
                options.add(option);
            }
        }
        dto.defaultValue = (String) m.get("defaultValue");
        dto.description = (String) m.get("description");
        dto.label = (String) m.get("label");
        dto.name = (String) m.get("name");
        dto.buttonFormElementOptionList = options;
        dto.required = b != null ? b : false;
        dto.type = ButtonFormType.fromObject(m.get("type"));
        return dto.name != null ? dto : null;
    }

    public static List<ButtonFormElementDTO> _fjs2List(List<Map<String, Object>> list) {
        List<ButtonFormElementDTO> elements = new ArrayList<>();
        for (Map m : list) {
            ButtonFormElementDTO element = ButtonFormElementDTO._fjs(m);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements;
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
        ButtonFormElementDTO other = (ButtonFormElementDTO) obj;
        if (defaultValue == null) {
            if (other.defaultValue != null) {
                return false;
            }
        } else if (!defaultValue.equals(other.defaultValue)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
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
        if (buttonFormElementOptionList == null) {
            if (other.buttonFormElementOptionList != null) {
                return false;
            }
        } else if (!buttonFormElementOptionList.equals(other.buttonFormElementOptionList)) {
            return false;
        }
        if (required != other.required) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public List<ButtonFormElementOptionDTO> getButtonFormElementOptionList() {
        return buttonFormElementOptionList;
    }

    public boolean getRequired() {
        return required;
    }

    public ButtonFormType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (defaultValue == null ? 0 : defaultValue.hashCode());
        result = prime * result + (description == null ? 0 : description.hashCode());
        result = prime * result + (label == null ? 0 : label.hashCode());
        result = prime * result + (name == null ? 0 : name.hashCode());
        result =
                prime * result
                        + (buttonFormElementOptionList == null ? 0 : buttonFormElementOptionList.hashCode());
        result = prime * result + (required ? 1231 : 1237);
        result = prime * result + (type == null ? 0 : type.hashCode());
        return result;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setButtonFormElementOptionList(List<ButtonFormElementOptionDTO> options) {
        this.buttonFormElementOptionList = options;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void setType(ButtonFormType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ButtonFormDTO [options="
                + buttonFormElementOptionList
                + ", name="
                + name
                + ", label="
                + label
                + ", defaultValue="
                + defaultValue
                + ", type="
                + type
                + ", required="
                + required
                + ", description="
                + description
                + "]";
    }
}
