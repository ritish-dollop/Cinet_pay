package com.cinetpay.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinetpay.model.Transaction;
import com.cinetpay.request.PaymentRequestDto;
import com.cinetpay.response.ApiResponse;
import com.cinetpay.service.CinetPayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final CinetPayService cinetPayService;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody PaymentRequestDto request) {
        try {
            Transaction tx = cinetPayService.initiatePayment(request);
            return ResponseEntity.ok(ApiResponse.ok("Payment initiated", Map.of(
                    "transactionId", tx.getTransactionId(),
                    "paymentUrl",    tx.getPaymentUrl(),
                    "paymentToken",  tx.getPaymentToken(),
                    "status",        tx.getStatus()
            )));
        } catch (Exception ex) {
            log.error("Initiation error: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/notify")
    public ResponseEntity<String> handleNotification(HttpServletRequest request) {
        String transactionId = request.getParameter("cpm_trans_id");
        log.info("Notification received | transactionId={}", transactionId);

        if (transactionId == null || transactionId.isBlank()) {
            return ResponseEntity.badRequest().body("Missing transaction ID");
        }

        try {
            Transaction tx = cinetPayService.verifyTransaction(transactionId);
            log.info("Notification processed | transactionId={} status={}", transactionId, tx.getStatus());
        } catch (Exception ex) {
            log.error("Notification error: {}", ex.getMessage());
        }

        // Always return 200 so CinetPay does not keep retrying
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/return")
    public ResponseEntity<?> handleReturn(@RequestParam(required = false) String transaction_id) {
        log.info("User returned | transactionId={}", transaction_id);

        if (transaction_id == null) {
            return ResponseEntity.ok(ApiResponse.error("No transaction ID in return"));
        }

        return cinetPayService.getTransaction(transaction_id)
                .<ResponseEntity<?>>map(tx -> ResponseEntity.ok(ApiResponse.ok("Payment status", Map.of(
                        "transactionId", tx.getTransactionId(),
                        "status",        tx.getStatus(),
                        "paymentMethod", tx.getPaymentMethod() != null ? tx.getPaymentMethod() : "N/A"
                ))))
                .orElse(ResponseEntity.ok(ApiResponse.error("Transaction not found")));
    }

    @GetMapping("/status/{transactionId}")
    public ResponseEntity<?> checkStatus(@PathVariable String transactionId) {
        try {
            Transaction tx = cinetPayService.verifyTransaction(transactionId);
            return ResponseEntity.ok(ApiResponse.ok("Transaction verified", Map.of(
                    "transactionId", tx.getTransactionId(),
                    "status",        tx.getStatus(),
                    "amount",        tx.getAmount() != null ? tx.getAmount() : 0,
                    "currency",      tx.getCurrency() != null ? tx.getCurrency() : "",
                    "paymentMethod", tx.getPaymentMethod() != null ? tx.getPaymentMethod() : "N/A"
            )));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }
}