package com.cinetpay.webhook.service;

import com.cinetpay.webhook.payload.WebhookPayload;

public interface WebhookService {

    void process(WebhookPayload payload, String rawSignature);
}