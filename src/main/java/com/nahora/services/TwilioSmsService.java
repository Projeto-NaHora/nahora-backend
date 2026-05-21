package com.nahora.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TwilioSmsService implements SmsService {

    private final String phoneNumber;

    public TwilioSmsService(String accountSid, String authToken, String phoneNumber) {
        Twilio.init(accountSid, authToken);
        this.phoneNumber = phoneNumber;
        log.info("TwilioSmsService inicializado com o número {}", phoneNumber);
    }

    @Override
    public void sendSms(String to, String body) {
        Message message = Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(phoneNumber),
                body
        ).create();
        log.info("SMS enviado via Twilio para {} — SID: {}", to, message.getSid());
    }
}
