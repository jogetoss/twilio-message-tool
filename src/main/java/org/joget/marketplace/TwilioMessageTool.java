package org.joget.marketplace;

import com.twilio.Twilio;
import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.form.service.FormUtil;
import org.joget.workflow.model.WorkflowAssignment;

public class TwilioMessageTool extends DefaultApplicationPlugin {

    private static final String METHOD_WHATSAPP = "WHATSAPP";

    @Override
    public Object execute(Map properties) {

        final WorkflowAssignment wfAssignment = (WorkflowAssignment) properties.get("workflowAssignment");
        final AppDefinition appDef = (AppDefinition) properties.get("appDef");

        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");

        String recordId;
        if (wfAssignment != null) {
            recordId = appService.getOriginProcessId(wfAssignment.getProcessId());
        } else {
            recordId = (String) properties.get("recordId");
        }

        String accountSID = (String) properties.get("accountSID");
        String authToken = (String) properties.get("authToken");
        String sendAs = (String) properties.get("sendAs");
        String fromNumber = (String) properties.get("fromNumber");
        String toNumber = (String) properties.get("toNumber");
        String message = (String) properties.get("message");

        Object[] urls = null;
        if (properties.get("urls") instanceof Object[]) {
            urls = (Object[]) properties.get("urls");
        }

        // Init Twilio
        try {
            Twilio.init(accountSID, authToken);
            String[] phoneNumberTokens = toNumber.split(";");
            List<String> recipientPhoneNumbers = new ArrayList<>();
            for (int i = 0; i < phoneNumberTokens.length; i++) {
                String recipientPhoneNumber = phoneNumberTokens[i];
                if (METHOD_WHATSAPP.equalsIgnoreCase(sendAs)) {
                    recipientPhoneNumber = "whatsapp:" + recipientPhoneNumber;
                }
                recipientPhoneNumbers.add(recipientPhoneNumber);
            }

            if (METHOD_WHATSAPP.equalsIgnoreCase(sendAs)) {
                fromNumber = "whatsapp:" + fromNumber;
            }

            for (String recipientPhoneNumber : recipientPhoneNumbers) {
                Message twilioMessage = Message.creator(new PhoneNumber(recipientPhoneNumber),
                        new PhoneNumber(fromNumber), message)
                        .create();
                LogUtil.info(getClassName(), "Message SID: " + twilioMessage.getSid());

                // send out the media
                List<URI> mediaUriList = new ArrayList<>();
                if (urls != null && urls.length > 0) {
                    for (Object o : urls) {
                        Map mapping = (HashMap) o;
                        String url = mapping.get("url").toString();
                        URI mediaUri = new URI(url);
                        Message twilioMediaMessage = Message.creator(new PhoneNumber(recipientPhoneNumber),
                                new PhoneNumber(fromNumber), "")
                                .setMediaUrl(mediaUri)
                                .create();
                        LogUtil.info(getClassName(), "Message SID: " + twilioMediaMessage.getSid());
                    }

                }
            }
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        }

        return null;
    }

    @Override
    public String getName() {
        return "Twilio Message Tool";
    }

    @Override
    public String getVersion() {
        return "8.0.0";
    }

    @Override
    public String getDescription() {
        return "Twilio message tool to send message - SMS & WhatsApp";
    }

    @Override
    public String getLabel() {
        return "Twilio Message Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/TwilioMessageTool.json", null, true, "messages/TwilioMessageTool");
    }

}
