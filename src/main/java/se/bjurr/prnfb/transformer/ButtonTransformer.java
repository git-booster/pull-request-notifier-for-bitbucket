package se.bjurr.prnfb.transformer;

import static se.bjurr.prnfb.Util.isNullOrEmpty;
import static se.bjurr.prnfb.presentation.dto.ButtonFormType.checkbox;
import static se.bjurr.prnfb.presentation.dto.ButtonFormType.radio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import se.bjurr.prnfb.http.NotificationResponse;
import se.bjurr.prnfb.presentation.dto.ButtonDTO;
import se.bjurr.prnfb.presentation.dto.ButtonFormElementDTO;
import se.bjurr.prnfb.presentation.dto.ButtonFormElementOptionDTO;
import se.bjurr.prnfb.presentation.dto.ButtonPressDTO;
import se.bjurr.prnfb.presentation.dto.NotificationResponseDTO;
import se.bjurr.prnfb.settings.PrnfbButton;
import se.bjurr.prnfb.settings.PrnfbButtonFormElement;
import se.bjurr.prnfb.settings.PrnfbButtonFormElementOption;

public class ButtonTransformer {
  private static ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.registerModule(new Jdk8Module());
  }

  public static ButtonDTO toButtonDto(PrnfbButton from) {
    ButtonDTO to = new ButtonDTO();
    to.setName(from.getName());
    to.setUserLevel(from.getUserLevel());
    to.setUuid(from.getUuid());
    to.setRedirectUrl(from.getRedirectUrl());
    to.setProjectKey(from.getProjectKey().orElse(null));
    to.setRepositorySlug(from.getRepositorySlug().orElse(null));
    to.setConfirmation(from.getConfirmation());
    to.setConfirmationText(from.getConfirmationText());
    to.setButtonFormList(toButtonFormDtoList(from.getButtonFormElementList()));
    try {
      String buttonFormDtoListString = objectMapper.writeValueAsString(to.getButtonFormList());
      to.setButtonFormListString(buttonFormDtoListString);
      return to;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize buttonFormDtoListString to JSON: " + e, e);
    }
  }

  private static List<ButtonFormElementDTO> toButtonFormDtoList(
      List<PrnfbButtonFormElement> buttonFormElementList) {
    List<ButtonFormElementDTO> to = new ArrayList<>();
    if (buttonFormElementList != null) {
      for (PrnfbButtonFormElement from : buttonFormElementList) {
        to.add(toDto(from));
      }
    }
    return to;
  }

  private static ButtonFormElementDTO toDto(PrnfbButtonFormElement from) {
    ButtonFormElementDTO to = new ButtonFormElementDTO();
    to.setDefaultValue(from.getDefaultValue());
    to.setDescription(from.getDescription());
    to.setLabel(from.getLabel());
    to.setName(from.getName());
    to.setRequired(from.getRequired());
    to.setType(from.getType());
    to.setButtonFormElementOptionList(toDto(from.getOptions()));
    return to;
  }

  private static List<ButtonFormElementOptionDTO> toDto(
      List<PrnfbButtonFormElementOption> options) {
    List<ButtonFormElementOptionDTO> to = new ArrayList<>();
    if (options != null) {
      for (PrnfbButtonFormElementOption from : options) {
        to.add(toDto(from));
      }
    }
    return to;
  }

  private static ButtonFormElementOptionDTO toDto(PrnfbButtonFormElementOption from) {
    ButtonFormElementOptionDTO to = new ButtonFormElementOptionDTO();
    to.setDefaultValue(from.getDefaultValue());
    to.setLabel(from.getLabel());
    to.setName(from.getName());
    return to;
  }

  public static List<ButtonDTO> toButtonDtoList(Iterable<PrnfbButton> allowedButtons) {
    List<ButtonDTO> to = new ArrayList<>();
    for (PrnfbButton from : allowedButtons) {
      to.add(toButtonDto(from));
    }
    return to;
  }

  public static PrnfbButton toPrnfbButton(ButtonDTO buttonDto) {
    List<ButtonFormElementDTO> buttonFormDto = buttonDto.getButtonFormList();
    if (buttonFormDto == null
        || buttonFormDto.isEmpty() && !isNullOrEmpty(buttonDto.getButtonFormListString())) {

      ObjectReader objectReader = objectMapper.readerForListOf(ButtonFormElementDTO.class);
      try {

        if (isNullOrEmpty(buttonDto.getButtonFormListString())) {
          buttonFormDto = null;
        } else {
          buttonFormDto = objectReader.readValue(buttonDto.getButtonFormListString());
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to parse JSON into List<ButtonFormDto>: " + e, e);
      }
    }
    validateButtonFormDTOList(buttonFormDto);
    List<PrnfbButtonFormElement> buttonFormElement = toPrnfbButtonElementList(buttonFormDto);
    return new PrnfbButton(
        buttonDto.getUUID(),
        buttonDto.getName(),
        buttonDto.getUserLevel(),
        buttonDto.getConfirmation(),
        buttonDto.getProjectKey().orElse(null),
        buttonDto.getRepositorySlug().orElse(null),
        buttonDto.getConfirmationText(),
        buttonDto.getRedirectUrl(),
        buttonFormElement);
  }

  public static void validateButtonFormDTOList(List<ButtonFormElementDTO> buttonFormDtoList)
      throws Error {
    if (buttonFormDtoList == null || buttonFormDtoList.isEmpty()) {
      return;
    }
    for (ButtonFormElementDTO buttonFormDto : buttonFormDtoList) {
      if (isNullOrEmpty(buttonFormDto.getLabel())) {
        throw new Error("The label must be set.");
      }
      if (isNullOrEmpty(buttonFormDto.getName())) {
        throw new Error("The name must be set.");
      }
      if ((buttonFormDto.getType() == radio || buttonFormDto.getType() == checkbox)
          && (buttonFormDto.getButtonFormElementOptionList() == null
              || buttonFormDto.getButtonFormElementOptionList().isEmpty())) {
        throw new Error("When adding radio buttons, options must also be defined.");
      }
    }
  }

  private static List<PrnfbButtonFormElement> toPrnfbButtonElementList(
      List<ButtonFormElementDTO> buttonFormDtoList) {
    List<PrnfbButtonFormElement> to = new ArrayList<>();
    if (buttonFormDtoList != null) {
      for (ButtonFormElementDTO from : buttonFormDtoList) {
        to.add(to(from));
      }
    }
    return to;
  }

  private static PrnfbButtonFormElement to(ButtonFormElementDTO from) {
    return new PrnfbButtonFormElement(
        from.getDefaultValue(),
        from.getDescription(),
        from.getLabel(),
        from.getName(),
        to(from.getButtonFormElementOptionList()),
        from.getRequired(),
        from.getType());
  }

  private static List<PrnfbButtonFormElementOption> to(List<ButtonFormElementOptionDTO> options) {
    List<PrnfbButtonFormElementOption> to = new ArrayList<>();
    if (options != null) {
      for (ButtonFormElementOptionDTO from : options) {
        to.add(to(from));
      }
    }
    return to;
  }

  private static PrnfbButtonFormElementOption to(ButtonFormElementOptionDTO from) {
    PrnfbButtonFormElementOption to =
        new PrnfbButtonFormElementOption(from.getLabel(), from.getName(), from.getDefaultValue());
    return to;
  }

  public static ButtonPressDTO toTriggerResultDto(
      PrnfbButton button, List<NotificationResponse> results) {
    List<NotificationResponseDTO> notificationResponses = new ArrayList<>();
    for (NotificationResponse from : results) {
      String content = null;
      int status = 0;
      URI uri = null;
      if (from.getHttpResponse() != null) {
        content = from.getHttpResponse().getContent();
        status = from.getHttpResponse().getStatus();
        uri = from.getHttpResponse().getUri();
      }
      UUID notification = from.getNotification();
      String notificationName = from.getNotificationName();
      notificationResponses.add(
          new NotificationResponseDTO(uri, content, status, notification, notificationName));
    }
    return new ButtonPressDTO(button.getConfirmation(), notificationResponses);
  }
}
