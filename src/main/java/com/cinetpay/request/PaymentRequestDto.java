package com.cinetpay.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class PaymentRequestDto {

	@NotNull(message = "Amount is required")
    @Min(value = 100, message = "Minimum amount is 100")
    private Integer amount;

    @NotBlank(message = "Currency is required")
    private String currency = "XOF";   // XOF, XAF, GNF, CDF

    // Optional: OM, WAVE, MTN, etc. — if not set, all methods shown
    private String paymentMethod;

    @NotBlank(message = "Designation (description) is required")
    private String designation;        // shown on the payment page

    // OTP code if required by the payment method
    private String otpCode;

    // Client info
    private String clientFirstName;
    private String clientLastName;
    private String clientPhoneNumber;
    private String clientEmail;

    // Override default lang per request (optional)
    private String lang = "fr";

    // true = pay directly without redirect (if supported)
    private boolean directPay = false;
}