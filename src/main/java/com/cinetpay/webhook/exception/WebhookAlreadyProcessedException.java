package com.cinetpay.webhook.exception;

/**
 * Thrown when a webhook for an already-terminal transaction is received.
 * Handled gracefully — signals idempotency guard triggered.
 */
public class WebhookAlreadyProcessedException extends RuntimeException {
    public WebhookAlreadyProcessedException(String transactionId) {
        super("Transaction already processed: " + transactionId);
    }
}