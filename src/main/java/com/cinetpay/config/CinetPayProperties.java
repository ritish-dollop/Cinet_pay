package com.cinetpay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cinetpay")
public class CinetPayProperties {
	private String apikey;
    private String apiPassword;   // API Password from the page
    private String baseUrl;
    private String successUrl;
    private String failedUrl;
    private String notifyUrl;
    private String lang = "fr";
    private boolean directPay = false;
    private String webhookSecret;
}