package com.cinetpay.webhook.exception;

/**
 * Thrown when CinetPay's HMAC-SHA256 signature does not match.
 */
public class WebhookSignatureException extends RuntimeException {
    public WebhookSignatureException(String message) {
        super(message);
    }
}