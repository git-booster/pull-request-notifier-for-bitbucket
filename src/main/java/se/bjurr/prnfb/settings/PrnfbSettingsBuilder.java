package se.bjurr.prnfb.settings;

import static se.bjurr.prnfb.Util.firstNotNull;
import static se.bjurr.prnfb.settings.PrnfbSettingsDataBuilder.prnfbSettingsDataBuilder;

import java.util.ArrayList;
import java.util.List;

public class PrnfbSettingsBuilder {
  public static PrnfbSettingsBuilder prnfbSettingsBuilder() {
    return new PrnfbSettingsBuilder();
  }

  public static PrnfbSettingsBuilder prnfbSettingsBuilder(PrnfbSettings settings) {
    return new PrnfbSettingsBuilder(settings);
  }

  private List<PrnfbButton> buttons;
  private List<PrnfbNotification> notifications;
  private PrnfbSettingsData prnfbSettingsData;

  private PrnfbSettingsBuilder() {
    this.notifications = new ArrayList<>();
    this.buttons = new ArrayList<>();
    this.prnfbSettingsData =
        prnfbSettingsDataBuilder() //
            .build();
  }

  private PrnfbSettingsBuilder(PrnfbSettings settings) {
    this.notifications = firstNotNull(settings.getNotifications(), new ArrayList<>());
    this.buttons = firstNotNull(settings.getButtons(), new ArrayList<>());
    this.prnfbSettingsData = settings.getPrnfbSettingsData();
  }

  public PrnfbSettings build() {
    return new PrnfbSettings(this);
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

  public PrnfbSettingsBuilder setButtons(List<PrnfbButton> buttons) {
    this.buttons = buttons;
    return this;
  }

  public PrnfbSettingsBuilder setNotifications(List<PrnfbNotification> notifications) {
    this.notifications = notifications;
    return this;
  }

  public PrnfbSettingsBuilder setPrnfbSettingsData(PrnfbSettingsData prnfbSettingsData) {
    this.prnfbSettingsData = prnfbSettingsData;
    return this;
  }

  public PrnfbSettingsBuilder withButton(PrnfbButton prnfbButton) {
    this.buttons.add(prnfbButton);
    return this;
  }

  public PrnfbSettingsBuilder withNotification(PrnfbNotification notification) {
    this.notifications.add(notification);
    return this;
  }
}
