package com.cinetpay.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Column(name = "payment_token")
    private String paymentToken;

    @Column(name = "payment_url", length = 500)
    private String paymentUrl;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "description")
    private String description;

    @Column(name = "metadata")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "webhook_received_at")
    private LocalDateTime webhookReceivedAt;

    public enum TransactionStatus {
        PENDING,
        ACCEPTED,
        REFUSED,
        CANCELLED,
        ERROR
    }
}