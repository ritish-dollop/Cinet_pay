package com.cinetpay.webhook.service.impl;


import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.cinetpay.config.CinetPayProperties;
import com.cinetpay.model.Transaction;
import com.cinetpay.service.CinetPayService;
import com.cinetpay.service.repository.TransactionRepository;
import com.cinetpay.webhook.exception.WebhookAlreadyProcessedException;
import com.cinetpay.webhook.exception.WebhookSignatureException;
import com.cinetpay.webhook.payload.WebhookPayload;
import com.cinetpay.webhook.service.WebhookService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final CinetPayProperties    props;
    private final CinetPayService       cinetPayService;
    private final TransactionRepository transactionRepository;

    // ── PUBLIC ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void process(WebhookPayload payload, String rawSignature) {

        String txId = payload.getCpm_trans_id();
        log.info("Webhook received | transactionId={} result={}", txId, payload.getCpm_result());

        // Step 1 — Signature verification
        if (props.getWebhookSecret() != null && !props.getWebhookSecret().isBlank()) {
            verifySignature(payload, rawSignature);
        } else {
            log.warn("Webhook secret not configured — skipping signature check. " +
                     "Set cinetpay.webhook-secret in application.yml");
        }

        // Step 2 — Idempotency guard
        transactionRepository.findById(txId).ifPresent(existing -> {
            if (isTerminal(existing.getStatus())) {
                log.info("Duplicate webhook ignored | transactionId={} status={}",
                         txId, existing.getStatus());
                throw new WebhookAlreadyProcessedException(txId);
            }
        });

        // Step 3 — Re-verify with CinetPay (never trust webhook body alone)
        Transaction tx = cinetPayService.verifyTransaction(txId);

        // Step 4 — Persist webhook timestamp
        tx.setWebhookReceivedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        log.info("Webhook processed | transactionId={} finalStatus={}", txId, tx.getStatus());

        // Step 5 — Business event dispatch
        dispatchBusinessEvent(tx);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────

    private void verifySignature(WebhookPayload payload, String receivedSignature) {
        if (receivedSignature == null || receivedSignature.isBlank()) {
            throw new WebhookSignatureException("Missing signature in webhook payload");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    props.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            byte[] digest   = mac.doFinal(payload.getCpm_trans_id().getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(digest);

            if (!expected.equalsIgnoreCase(receivedSignature.trim())) {
                log.warn("Signature mismatch | transactionId={} expected={} received={}",
                         payload.getCpm_trans_id(), expected, receivedSignature);
                throw new WebhookSignatureException("Signature verification failed");
            }
            log.debug("Signature verified OK | transactionId={}", payload.getCpm_trans_id());

        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new RuntimeException("HMAC initialisation error", ex);
        }
    }

    /**
     * Returns true when the transaction is in a final, non-retryable state.
     */
    private boolean isTerminal(Transaction.TransactionStatus status) {
        return status == Transaction.TransactionStatus.ACCEPTED
            || status == Transaction.TransactionStatus.REFUSED
            || status == Transaction.TransactionStatus.CANCELLED;
    }

    /**
     * Hook for downstream business logic.
     * Inject OrderService / EmailService / ApplicationEventPublisher here as needed.
     */
    private void dispatchBusinessEvent(Transaction tx) {
        switch (tx.getStatus()) {
            case ACCEPTED ->
                log.info("✅ Payment ACCEPTED | transactionId={} amount={} {}",
                         tx.getTransactionId(), tx.getAmount(), tx.getCurrency());
                // TODO: fulfil order, send confirmation email, etc.

            case REFUSED ->
                log.warn("❌ Payment REFUSED | transactionId={} reason={}",
                         tx.getTransactionId(), tx.getErrorMessage());
                // TODO: notify customer, release reserved stock, etc.

            case CANCELLED ->
                log.info("⚠️ Payment CANCELLED | transactionId={}", tx.getTransactionId());
                // TODO: release reservation, update UI state, etc.

            default ->
                log.error("Unknown status after webhook | transactionId={} status={}",
                          tx.getTransactionId(), tx.getStatus());
        }
    }
}