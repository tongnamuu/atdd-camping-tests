package com.camping.tests.steps;

import com.camping.tests.support.TestConfig;
import com.camping.tests.support.TestContext;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만약;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 결제 서비스 통합 테스트 스텝 정의
 * 대상 URL: PAYMENTS_BASE_URL 환경 변수 (기본값 http://localhost:9090)
 */
public class PaymentSteps {

    private final TestContext context;

    private static final String BASE_URL = TestConfig.PAYMENTS_BASE_URL;
    private static final String SECRET_KEY = "test_sk_dummy";

    public PaymentSteps(TestContext context) {
        this.context = context;
    }

    @만약("결제 키 {string} 주문 ID {string} 금액 {int}원으로 결제를 생성한다")
    public void 결제를_생성한다(String paymentKey, String orderId, int amount) {
        context.setPaymentKey(paymentKey);
        context.setOrderId(orderId);
        context.setAmount(amount);

        String body = String.format(
                "{\"paymentKey\":\"%s\",\"orderId\":\"%s\",\"amount\":%d}",
                paymentKey, orderId, amount);

        Response response = authenticated()
                .body(body)
                .post(BASE_URL + "/v1/payments");
        context.setLastResponse(response);
    }

    @그리고("결제 키 {string} 주문 ID {string} 금액 {int}원으로 결제를 승인한다")
    public void 결제를_승인한다(String paymentKey, String orderId, int amount) {
        context.setPaymentKey(paymentKey);
        context.setAmount(amount);

        String body = String.format(
                "{\"paymentKey\":\"%s\",\"orderId\":\"%s\",\"amount\":%d}",
                paymentKey, orderId, amount);

        Response response = authenticated()
                .body(body)
                .post(BASE_URL + "/v1/payments/confirm");
        context.setLastResponse(response);
    }

    @그리고("결제 키 {string}의 결제를 {int}원으로 취소한다")
    public void 결제를_취소한다(String paymentKey, int cancelAmount) {
        String body = String.format(
                "{\"cancelReason\":\"테스트 취소\",\"cancelAmount\":%d}", cancelAmount);

        Response response = authenticated()
                .body(body)
                .post(BASE_URL + "/v1/payments/" + paymentKey + "/cancel");
        context.setLastResponse(response);
    }

    @만약("인증 없이 결제를 생성한다")
    public void 인증없이_결제를_생성한다() {
        String body = "{\"paymentKey\":\"pay_noauth\",\"orderId\":\"ord_noauth\",\"amount\":1000}";

        Response response = RestAssured.given()
                .contentType("application/json")
                .body(body)
                .post(BASE_URL + "/v1/payments");
        context.setLastResponse(response);
    }

    private RequestSpecification authenticated() {
        String token = Base64.getEncoder()
                .encodeToString((SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
        return RestAssured.given()
                .header("Authorization", "Basic " + token)
                .contentType("application/json");
    }
}
