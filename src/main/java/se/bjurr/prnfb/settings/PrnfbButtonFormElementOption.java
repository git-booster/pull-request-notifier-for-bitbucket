package se.bjurr.prnfb.settings;

import static se.bjurr.prnfb.Util.checkNotNull;

public class PrnfbButtonFormElementOption {
  private String label;
  private String name;
  private Boolean defaultValue;

  public PrnfbButtonFormElementOption() {}

  public PrnfbButtonFormElementOption(String label, String name, Boolean defaultValue) {
    this.label = checkNotNull(label, "label");
    this.name = checkNotNull(name, "name");
    this.defaultValue = defaultValue;
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

  public void setDefaultValue(Boolean b) {
    this.defaultValue = b;
  }

  public void setLabel(String s) {
    this.label = s;
  }

  public void setName(String s) {
    this.name = s;
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
    PrnfbButtonFormElementOption other = (PrnfbButtonFormElementOption) obj;
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
