package com.cinetpay.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.cinetpay.config.CinetPayProperties;
import com.cinetpay.model.Transaction;
import com.cinetpay.request.PaymentRequestDto;
import com.cinetpay.response.CinetPayInitResponse;
import com.cinetpay.service.CinetPayService;
import com.cinetpay.service.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CinetPayServiceImpl implements CinetPayService {

	private final CinetPayProperties props;
	private final RestTemplate restTemplate;
	private final TransactionRepository transactionRepository;

	// ── 1. INITIATE PAYMENT ───────────────────────────────────────────────

	@Override
	public Transaction initiatePayment(PaymentRequestDto request) {
		// STEP 1 → Generate Access Token
		String accessToken = generateAccessToken();
		// STEP 2 → Generate Transaction ID
		String transactionId = generateTransactionId();
		// STEP 3 → Build Payload
		Map<String, Object> body = buildInitPayload(transactionId, request);

		log.info("Initiating payment | transactionId={} amount={} {}", transactionId, request.getAmount(),
				request.getCurrency());

		// STEP 4 → Call Payment API with Bearer Token
		CinetPayInitResponse cinetResponse = post(props.getBaseUrl() + "/v1/payment", body, CinetPayInitResponse.class,
				accessToken);

		System.err.println(cinetResponse);
		if (cinetResponse == null || !cinetResponse.isSuccess()) {
			String msg = cinetResponse != null ? cinetResponse.getStatus() : "null response";
			log.error("CinetPay init failed: {}", msg);
			throw new RuntimeException("Payment initialization failed: " + msg);
		}

		Transaction tx = Transaction.builder().transactionId(transactionId)
				.paymentToken(cinetResponse.getPaymentToken()).paymentUrl(cinetResponse.getPaymentUrl())
				.amount(request.getAmount()).currency(request.getCurrency()).description(request.getDesignation())
				.status(Transaction.TransactionStatus.PENDING).createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now()).build();

		transactionRepository.save(tx);
		log.info("Payment URL ready | transactionId={} url={}", transactionId, cinetResponse.getPaymentUrl());
		return tx;
	}

	// ── 2. VERIFY TRANSACTION ─────────────────────────────────────────────

	@Override
	public Transaction verifyTransaction(String transactionId) {
		log.info("Verifying | transactionId={}", transactionId);
		Map<String, Object> body = new HashMap<>();
		body.put("transaction_id", transactionId);
		String accessToken = generateAccessToken();
		Map<?, ?> cinetResponse = post(props.getBaseUrl() + "/v1/payment/check", body, Map.class, accessToken);
		if (cinetResponse == null) {
			throw new RuntimeException("No response from CinetPay verification");
		}
		String code = String.valueOf(cinetResponse.get("code"));
		Transaction tx = transactionRepository.findById(transactionId)
				.orElse(Transaction.builder().transactionId(transactionId).build());
		if ("200".equals(code) || "00".equals(code)) {
			@SuppressWarnings("unchecked")
			Map<String, Object> data = (Map<String, Object>) cinetResponse.get("data");
			String status = data != null ? String.valueOf(data.get("status")) : "UNKNOWN";
			tx.setStatus(mapStatus(status));
			tx.setPaymentMethod(data != null ? String.valueOf(data.get("payment_method")) : null);
			tx.setUpdatedAt(LocalDateTime.now());
		} else {
			tx.setStatus(Transaction.TransactionStatus.ERROR);
			tx.setErrorMessage(String.valueOf(cinetResponse.get("message")));
			tx.setUpdatedAt(LocalDateTime.now());
		}
		transactionRepository.save(tx);
		return tx;
	}

	// ── 3. GET TRANSACTION ────────────────────────────────────────────────

	@Override
	public Optional<Transaction> getTransaction(String transactionId) {
		return transactionRepository.findById(transactionId);
	}

	// ── PRIVATE HELPERS ───────────────────────────────────────────────────

	private Map<String, Object> buildInitPayload(String transactionId, PaymentRequestDto req) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("currency", req.getCurrency());
		if (req.getPaymentMethod() != null && !req.getPaymentMethod().isBlank()) {
			body.put("payment_method", req.getPaymentMethod());
		}
		body.put("merchant_transaction_id", transactionId);
		body.put("amount", req.getAmount());
		body.put("success_url", props.getSuccessUrl());
		body.put("failed_url", props.getFailedUrl());
		body.put("notify_url", props.getNotifyUrl());
		body.put("lang", req.getLang() != null ? req.getLang() : "fr");
		body.put("designation", req.getDesignation());
		body.put("client_first_name", req.getClientFirstName());
		body.put("client_last_name", req.getClientLastName());
		body.put("client_phone_number", req.getClientPhoneNumber());
		body.put("client_email", req.getClientEmail());
		body.put("direct_pay", req.isDirectPay());
		if (req.getOtpCode() != null && !req.getOtpCode().isBlank()) {
			body.put("otp_code", req.getOtpCode());
		}
		return body;
	}

	private <T> T post(String url, Object body, Class<T> responseType, String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		// IMPORTANT
		headers.setBearerAuth(token);
		try {
			return restTemplate.postForEntity(url, new HttpEntity<>(body, headers), responseType).getBody();
		} catch (Exception ex) {
			log.error("CinetPay HTTP call failed | url={} error={}", url, ex.getMessage());
			throw new RuntimeException("CinetPay API call failed: " + ex.getMessage(), ex);
		}
	}

	private Transaction.TransactionStatus mapStatus(String status) {
		return switch (status.toUpperCase()) {
		case "ACCEPTED" -> Transaction.TransactionStatus.ACCEPTED;
		case "REFUSED" -> Transaction.TransactionStatus.REFUSED;
		case "CANCELLED" -> Transaction.TransactionStatus.CANCELLED;
		default -> Transaction.TransactionStatus.ERROR;
		};
	}

	private String generateTransactionId() {
		return "TXN-" + System.currentTimeMillis() + "-"
				+ UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
	}

	private String generateAccessToken() {
		String url = props.getBaseUrl() + "/v1/oauth/login";
		Map<String, Object> loginBody = new LinkedHashMap<>();
		loginBody.put("api_key", props.getApikey());
		loginBody.put("api_password", props.getApiPassword());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(loginBody, headers);
		try {
			ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
			Map body = response.getBody();
			if (body == null || body.get("access_token") == null) {
				throw new RuntimeException("Unable to generate access token");
			}
			return body.get("access_token").toString();
		} catch (Exception ex) {
			log.error("Token generation failed: {}", ex.getMessage());
			throw new RuntimeException("CinetPay Login Failed: " + ex.getMessage());
		}
	}
}