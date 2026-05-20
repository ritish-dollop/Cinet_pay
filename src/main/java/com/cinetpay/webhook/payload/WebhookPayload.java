package com.cinetpay.webhook.payload;

import lombok.Data;

@Data
public class WebhookPayload {

    private String cpm_trans_id;
    private String cpm_result;
    private String cpm_payid;
    private String cpm_amount;
    private String cpm_currency;
    private String payment_method;
    private String cel_phone_num;
    private String cpm_error_message;
    private String cpm_page_action;
    private String signature;
}