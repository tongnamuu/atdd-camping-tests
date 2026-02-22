package com.camping.tests.steps;

import com.camping.tests.support.TestConfig;
import com.camping.tests.support.TestContext;
import io.cucumber.java.ko.만약;
import io.restassured.RestAssured;
import io.restassured.response.Response;

/**
 * 키오스크 스모크 테스트 스텝 정의
 */
public class SmokeSteps {

    private final TestContext context;

    public SmokeSteps(TestContext context) {
        this.context = context;
    }

    @만약("키오스크 헬스 체크를 요청한다")
    public void 키오스크_헬스_체크() {
        Response response = RestAssured.given()
                .get(TestConfig.KIOSK_BASE_URL + "/health");
        context.setLastResponse(response);
    }
}
