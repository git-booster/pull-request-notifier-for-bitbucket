package se.bjurr.prnfb;

import com.atlassian.bitbucket.pull.PullRequestState;
import org.junit.Test;
import se.bjurr.prnfb.http.UrlInvoker;
import se.bjurr.prnfb.listener.PrnfbPullRequestAction;
import se.bjurr.prnfb.presentation.dto.ButtonDTO;
import se.bjurr.prnfb.presentation.dto.ButtonFormElementDTO;
import se.bjurr.prnfb.presentation.dto.ButtonFormType;
import se.bjurr.prnfb.presentation.dto.ON_OR_OFF;
import se.bjurr.prnfb.service.PrnfbRenderer;
import se.bjurr.prnfb.settings.PrnfbButton;
import se.bjurr.prnfb.settings.PrnfbButtonFormElement;
import se.bjurr.prnfb.settings.PrnfbButtonFormElementOption;
import se.bjurr.prnfb.settings.PrnfbHeader;
import se.bjurr.prnfb.settings.PrnfbNotification;
import se.bjurr.prnfb.settings.PrnfbNotificationBuilder;
import se.bjurr.prnfb.settings.PrnfbSettings;
import se.bjurr.prnfb.settings.PrnfbSettingsBuilder;
import se.bjurr.prnfb.settings.PrnfbSettingsData;
import se.bjurr.prnfb.settings.PrnfbSettingsDataBuilder;
import se.bjurr.prnfb.settings.TRIGGER_IF_MERGE;
import se.bjurr.prnfb.settings.USER_LEVEL;
import se.bjurr.prnfb.transformer.ButtonTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JsonTest {

    @Test
    public void test() throws Exception {

        UUID u1 = UUID.fromString("11111111-1111-1111-eeee-ffffffffffff");
        UUID u2 = UUID.fromString("22222222-2222-2222-eeee-ffffffffffff");
        UUID u3 = UUID.fromString("33333333-3333-3333-eeee-ffffffffffff");

        PrnfbButtonFormElementOption opt1 = new PrnfbButtonFormElementOption("a", "b", true);
        PrnfbButtonFormElementOption opt2 = new PrnfbButtonFormElementOption("c", "d", false);
        List<PrnfbButtonFormElementOption> list = new ArrayList<>();
        list.add(opt1);
        list.add(opt2);
        PrnfbButtonFormElement pbfe =
                new PrnfbButtonFormElement(
                        "default", "description", "label", "name", list, true, ButtonFormType.textarea);

        List<PrnfbButtonFormElement> elements = new ArrayList<>();
        elements.add(pbfe);

        PrnfbButton button1 =
                new PrnfbButton(
                        u1,
                        "name1",
                        USER_LEVEL.ADMIN,
                        ON_OR_OFF.on,
                        "projectKey1",
                        "repoSlug1",
                        "confirmationText1",
                        "redirectUrl1",
                        elements);
        PrnfbButton button2 =
                new PrnfbButton(
                        u2,
                        "name2",
                        USER_LEVEL.EVERYONE,
                        ON_OR_OFF.off,
                        "projectKey2",
                        "repoSlug2",
                        "confirmationText2",
                        "redirectUrl2",
                        elements);
        PrnfbButton button3 =
                new PrnfbButton(
                        u3,
                        "name3",
                        USER_LEVEL.SYSTEM_ADMIN,
                        ON_OR_OFF.on,
                        "projectKey3",
                        "repoSlug3",
                        "confirmationText3",
                        "redirectUrl3",
                        elements);
        List<PrnfbButton> buttons = new ArrayList<>();
        buttons.add(button1);
        buttons.add(button2);
        buttons.add(button3);

        List<PrnfbHeader> headers = new ArrayList<>();
        headers.add(new PrnfbHeader("n1", "v1"));
        headers.add(new PrnfbHeader("n2", "v2"));

        List<PullRequestState> states = new ArrayList<>();
        states.add(PullRequestState.OPEN);
        states.add(PullRequestState.MERGED);
        states.add(PullRequestState.DECLINED);

        List<PrnfbPullRequestAction> actions = new ArrayList<>();
        actions.add(PrnfbPullRequestAction.DECLINED);
        actions.add(PrnfbPullRequestAction.BUTTON_TRIGGER);
        actions.add(PrnfbPullRequestAction.RESCOPED_FROM);

        PrnfbNotificationBuilder b =
                new PrnfbNotificationBuilder(
                        "filterRegexp",
                        "filterString",
                        headers,
                        "injectionUrl",
                        "injectionUrlRegexp",
                        "variableName",
                        "variableRegex",
                        UrlInvoker.HTTP_METHOD.GET,
                        "name",
                        "password",
                        "postContent",
                        "projectKey",
                        "proxyPassword",
                        12345,
                        "proxyServer",
                        "proxyUser",
                        "repositorySlug",
                        TRIGGER_IF_MERGE.ALWAYS,
                        states,
                        actions,
                        true,
                        "https://url.com/",
                        "user",
                        u1,
                        PrnfbRenderer.ENCODE_FOR.NONE,
                        "proxySchema",
                        "httpVersion");
        PrnfbNotification n = b.build();
        List<PrnfbNotification> notifications = new ArrayList<>();
        notifications.add(n);

        PrnfbSettingsDataBuilder bb = PrnfbSettingsDataBuilder.prnfbSettingsDataBuilder();
        bb.setAdminRestriction(USER_LEVEL.EVERYONE);
        bb.setKeyStore("keyStore");
        bb.setKeyStorePassword("keyStorePassword");
        bb.setShouldAcceptAnyCertificate(true);
        bb.setKeyStoreType("keyStoreType");
        PrnfbSettingsData data = bb.build();

        PrnfbSettingsBuilder bbb = PrnfbSettingsBuilder.prnfbSettingsBuilder();
        bbb.setButtons(buttons);
        bbb.setNotifications(notifications);
        bbb.setPrnfbSettingsData(data);

        PrnfbSettings settings = bbb.build();

        Map<String, Object> m = settings._2js();
        String json = Java2Json.format(true, m);
        String json1 = json;

        m = Java2Json.parseToMap(json);
        settings = PrnfbSettings._fjs(m);
        m = settings._2js();
        json = Java2Json.format(true, m);
        String json2 = json;

        ButtonDTO buttonDto = ButtonTransformer.toButtonDto(button1);
        List<ButtonFormElementDTO> dtos = buttonDto.getButtonFormList();

        String myString = Java2Json._2ListJsonString((List) dtos);

        List<Map<String, Object>> l = (List) Java2Json.parse(myString);

        List<ButtonFormElementDTO> buttonFormDto = ButtonFormElementDTO._fjs2List(l);
        myString = Java2Json._2ListJsonString((List) buttonFormDto);

    }
}
