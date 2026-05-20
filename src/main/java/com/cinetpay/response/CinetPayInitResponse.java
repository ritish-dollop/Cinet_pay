package com.cinetpay.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinetPayInitResponse {
	@JsonProperty("code")
    private Integer code;

    @JsonProperty("status")
    private String status;

    @JsonProperty("merchant_transaction_id")
    private String merchantTransactionId;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("payment_token")
    private String paymentToken;

    @JsonProperty("payment_url")
    private String paymentUrl;

    @JsonProperty("details")
    private Details details;

    // Success = HTTP 200 and status "OK"
    public boolean isSuccess() {
        return "OK".equalsIgnoreCase(status) && Integer.valueOf(200).equals(code);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Details {

        @JsonProperty("code")
        private Integer code;

        @JsonProperty("status")
        private String status;       // INITIATED

        @JsonProperty("message")
        private String message;

        @JsonProperty("must_be_redirected")
        private boolean mustBeRedirected;
    }
}