package com.cinetpay.webhook.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cinetpay.webhook.exception.WebhookAlreadyProcessedException;
import com.cinetpay.webhook.exception.WebhookSignatureException;
import com.cinetpay.webhook.payload.WebhookPayload;
import com.cinetpay.webhook.service.WebhookService;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;   

    @PostMapping("/cinetpay")
    public ResponseEntity<String> handleWebhook(
            @ModelAttribute WebhookPayload payload,
            HttpServletRequest request) {

        String signature = request.getParameter("signature");
        log.info("Webhook hit | transactionId={} signaturePrefix={}",
                 payload.getCpm_trans_id(),
                 signature != null ? signature.substring(0, Math.min(8, signature.length())) + "..." : "null");

        try {
            webhookService.process(payload, signature);

        } catch (WebhookAlreadyProcessedException ex) {
            log.info("Duplicate webhook acknowledged | {}", ex.getMessage());

        } catch (WebhookSignatureException ex) {
            log.error("Webhook signature invalid | {}", ex.getMessage());

        } catch (Exception ex) {
            log.error("Webhook processing error | transactionId={} error={}",
                      payload.getCpm_trans_id(), ex.getMessage(), ex);
        }

        return ResponseEntity.ok("OK");
    }
}