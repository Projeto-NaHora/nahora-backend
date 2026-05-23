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
                new PhoneNumber(toE164Brazil(to)),
                new PhoneNumber(phoneNumber),
                body
        ).create();
        log.info("SMS enviado via Twilio para {} — SID: {}", to, message.getSid());
    }

    // Twilio requires E.164; normalize Brazilian numbers that arrive without country code
    private String toE164Brazil(String numero) {
        if (numero.startsWith("+")) {
            return numero;
        }
        String digits = numero.replaceAll("[^0-9]", "");
        if (digits.length() == 13 && digits.startsWith("55")) {
            return "+" + digits;
        }
        return "+55" + digits;
    }
}
