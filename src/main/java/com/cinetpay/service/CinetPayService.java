package com.cinetpay.service;

import java.util.Optional;

import com.cinetpay.model.Transaction;
import com.cinetpay.request.PaymentRequestDto;

public interface CinetPayService {

    /**
     * Initiates a payment with CinetPay and returns the transaction
     * containing the payment URL to redirect the user to.
     */
    Transaction initiatePayment(PaymentRequestDto request);

    /**
     * Verifies a transaction status directly with CinetPay.
     * Always call this from your notify endpoint — never trust return_url params.
     */
    Transaction verifyTransaction(String transactionId);

    /**
     * Fetches a transaction from local store by its ID.
     */
    Optional<Transaction> getTransaction(String transactionId);
}