package com.camping.tests.support;

import io.restassured.response.Response;

/**
 * 시나리오 내 단계 간 공유 상태 (PicoContainer를 통해 DI)
 */
public class TestContext {

    private Response lastResponse;
    private String paymentKey;
    private String orderId;
    private int amount;

    public Response getLastResponse() { return lastResponse; }
    public void setLastResponse(Response lastResponse) { this.lastResponse = lastResponse; }

    public String getPaymentKey() { return paymentKey; }
    public void setPaymentKey(String paymentKey) { this.paymentKey = paymentKey; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
}
