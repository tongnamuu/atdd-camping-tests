package com.camping.tests.steps;

import com.camping.tests.support.TestConfig;
import com.camping.tests.support.TestContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만약;
import io.restassured.RestAssured;
import io.restassured.response.Response;

/**
 * 키오스크 ATDD 스텝 정의
 * 대상 URL: KIOSK_BASE_URL 환경 변수 (기본값 http://localhost:8080)
 * 관리자 서비스는 WireMock으로 모의됨
 */
public class KioskSteps {

    private final TestContext context;

    private static final String BASE_URL = TestConfig.KIOSK_BASE_URL;

    public KioskSteps(TestContext context) {
        this.context = context;
    }

    @만약("상품 목록을 조회한다")
    public void 상품_목록을_조회한다() {
        Response response = RestAssured.given()
                .get(BASE_URL + "/api/products");
        context.setLastResponse(response);
    }

    @만약("상품 {string} {int}개를 담아 결제를 생성한다")
    public void 상품을_담아_결제를_생성한다(String productName, int quantity) {
        int unitPrice = resolveUnitPrice(productName);
        long productId = resolveProductId(productName);
        int amount = unitPrice * quantity;
        context.setAmount(amount);

        String body = String.format(
                "{\"items\":[{\"productId\":%d,\"productName\":\"%s\",\"unitPrice\":%d,\"quantity\":%d}]," +
                "\"paymentMethod\":\"CARD\"}",
                productId, productName, unitPrice, quantity);

        Response response = RestAssured.given()
                .contentType("application/json")
                .body(body)
                .post(BASE_URL + "/api/payments");
        context.setLastResponse(response);

        if (response.getStatusCode() == 200) {
            context.setPaymentKey(response.jsonPath().getString("paymentKey"));
            context.setOrderId(response.jsonPath().getString("orderId"));
        }
    }

    @만약("생성된 결제를 승인한다")
    public void 생성된_결제를_승인한다() {
        String body = String.format(
                "{\"paymentKey\":\"%s\",\"orderId\":\"%s\",\"amount\":%d,\"items\":[]}",
                context.getPaymentKey(), context.getOrderId(), context.getAmount());

        Response response = RestAssured.given()
                .contentType("application/json")
                .body(body)
                .post(BASE_URL + "/api/payments/confirm");
        context.setLastResponse(response);
    }

    private int resolveUnitPrice(String productName) {
        return switch (productName) {
            case "장작팩" -> 10000;
            case "생수(2L)" -> 2000;
            case "라면 세트" -> 4000;
            default -> 1000;
        };
    }

    private long resolveProductId(String productName) {
        return switch (productName) {
            case "장작팩" -> 2L;
            case "생수(2L)" -> 8L;
            case "라면 세트" -> 9L;
            default -> 1L;
        };
    }
}
