package com.nahora.config;

import com.nahora.services.MockSmsService;
import com.nahora.services.SmsService;
import com.nahora.services.TwilioSmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class SmsConfig {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.phone-number:}")
    private String phoneNumber;

    @Bean
    public SmsService smsService() {
        if (StringUtils.hasText(accountSid) && StringUtils.hasText(authToken) && StringUtils.hasText(phoneNumber)) {
            return new TwilioSmsService(accountSid, authToken, phoneNumber);
        }
        log.warn("Variáveis Twilio não configuradas — usando MockSmsService");
        return new MockSmsService();
    }
}
