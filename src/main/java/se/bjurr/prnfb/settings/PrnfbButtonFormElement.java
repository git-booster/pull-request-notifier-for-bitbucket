package se.bjurr.prnfb.settings;

import static se.bjurr.prnfb.Util.checkNotNull;
import static se.bjurr.prnfb.Util.emptyToNull;
import static se.bjurr.prnfb.Util.firstNotNull;

import java.util.ArrayList;
import java.util.List;
import se.bjurr.prnfb.presentation.dto.ButtonFormType;

public class PrnfbButtonFormElement {
  private String defaultValue;
  private String description;
  private String label;
  private String name;
  private List<PrnfbButtonFormElementOption> buttonFormElementOptionList;
  private boolean required;
  private ButtonFormType type;

  public PrnfbButtonFormElement(
      String defaultValue,
      String description,
      String label,
      String name,
      List<PrnfbButtonFormElementOption> options,
      boolean required,
      ButtonFormType type) {
    this.defaultValue = emptyToNull(defaultValue);
    this.description = emptyToNull(description);
    this.label = checkNotNull(label, "label");
    this.name = checkNotNull(name, "name");
    this.buttonFormElementOptionList = firstNotNull(options, new ArrayList<>());
    this.required = required;
    this.type = checkNotNull(type);
  }

  public PrnfbButtonFormElement() {}

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
    PrnfbButtonFormElement other = (PrnfbButtonFormElement) obj;
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

  public List<PrnfbButtonFormElementOption> getOptions() {
    return buttonFormElementOptionList;
  }

  public boolean getRequired() {
    return required;
  }

  public ButtonFormType getType() {
    return type;
  }

  public void setDefaultValue(String s) {
    this.defaultValue = s;
  }

  public void setDescription(String s) {
    this.description = s;
  }

  public void setLabel(String s) {
    this.label = s;
  }

  public void setName(String s) {
    this.name = s;
  }

  public void setOptions(List<PrnfbButtonFormElementOption> list) {
    this.buttonFormElementOptionList = list;
  }

  public void setRequired(boolean b) {
    this.required = b;
  }

  public void setType(ButtonFormType type) {
    this.type = type;
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
