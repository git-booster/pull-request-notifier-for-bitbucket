package se.bjurr.prnfb.settings;

import static se.bjurr.prnfb.Util.checkNotNull;

import java.util.ArrayList;
import java.util.List;

public class PrnfbSettings {
  public static final String UNCHANGED = "KEEP_THIS_TO_LEAVE_UNCHANGED";
  private List<PrnfbButton> buttons;
  private List<PrnfbNotification> notifications = new ArrayList<>();
  private PrnfbSettingsData prnfbSettingsData;

  public PrnfbSettings() {}

  public PrnfbSettings(PrnfbSettingsBuilder builder) {
    this.notifications = checkNotNull(builder.getNotifications());
    this.buttons = checkNotNull(builder.getButtons());
    this.prnfbSettingsData = checkNotNull(builder.getPrnfbSettingsData(), "prnfbSettingsData");
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
    PrnfbSettings other = (PrnfbSettings) obj;
    if (this.buttons == null) {
      if (other.buttons != null) {
        return false;
      }
    } else if (!this.buttons.equals(other.buttons)) {
      return false;
    }
    if (this.notifications == null) {
      if (other.notifications != null) {
        return false;
      }
    } else if (!this.notifications.equals(other.notifications)) {
      return false;
    }
    if (this.prnfbSettingsData == null) {
      if (other.prnfbSettingsData != null) {
        return false;
      }
    } else if (!this.prnfbSettingsData.equals(other.prnfbSettingsData)) {
      return false;
    }
    return true;
  }

  public List<PrnfbButton> getButtons() {
    return this.buttons;
  }

  public List<PrnfbNotification> getNotifications() {
    return this.notifications;
  }

  public PrnfbSettingsData getPrnfbSettingsData() {
    return this.prnfbSettingsData;
  }

  public void setButtons(List<PrnfbButton> list) {
    this.buttons = list;
  }

  public void setNotifications(List<PrnfbNotification> list) {
    this.notifications = list;
  }

  public void setPrnfbSettingsData(PrnfbSettingsData data) {
    this.prnfbSettingsData = data;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (this.buttons == null ? 0 : this.buttons.hashCode());
    result = prime * result + (this.notifications == null ? 0 : this.notifications.hashCode());
    result =
        prime * result + (this.prnfbSettingsData == null ? 0 : this.prnfbSettingsData.hashCode());
    return result;
  }
}
