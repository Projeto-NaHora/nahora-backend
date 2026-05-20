package com.nahora.services;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockSmsService implements SmsService {

    @Override
    public void sendSms(String to, String body) {
        log.info("Mock SMS -> para {}: {}", to, body);
    }
}
