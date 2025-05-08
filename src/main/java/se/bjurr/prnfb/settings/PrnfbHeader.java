package se.bjurr.prnfb.settings;

import static se.bjurr.prnfb.Util.checkNotNull;
import static se.bjurr.prnfb.Util.emptyToNull;
import static se.bjurr.prnfb.Util.nullToEmpty;

public class PrnfbHeader {

  private String name;
  private String value;

  public PrnfbHeader() {}

  public PrnfbHeader(String name, String value) {
    this.name = checkNotNull(emptyToNull(nullToEmpty(name).trim()), "Header name must be set");
    this.value = checkNotNull(emptyToNull(nullToEmpty(value).trim()), "Header value must be set");
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
