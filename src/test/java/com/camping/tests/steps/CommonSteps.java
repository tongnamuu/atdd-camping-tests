package com.camping.tests.steps;

import com.camping.tests.support.TestContext;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.그러면;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 결제/키오스크 공통 검증 스텝 정의.
 *
 * Cucumber에서 @만약/@그리고/@그러면 등 키워드 애너테이션은
 * 문서화 용도일 뿐, 스텝 매칭은 오직 패턴 텍스트로만 이루어진다.
 * 같은 텍스트 패턴을 두 개의 메서드에 등록하면 DuplicateStepDefinitionException이 발생한다.
 */
public class CommonSteps {

    private final TestContext context;

    public CommonSteps(TestContext context) {
        this.context = context;
    }

    @그러면("응답 상태 코드는 {int}이다")
    public void 응답_상태_코드_확인(int expectedStatus) {
        assertThat(context.getLastResponse().getStatusCode()).isEqualTo(expectedStatus);
    }

    @그리고("결제 상태는 {string}이다")
    public void 결제_상태_확인(String expectedStatus) {
        String actual = context.getLastResponse().jsonPath().getString("status");
        assertThat(actual).isEqualTo(expectedStatus);
    }

    // 하나의 메서드로 "그러면/그리고" 양쪽 키워드에서 모두 매칭됨
    @그리고("결제 생성에 성공한다")
    public void 결제_생성에_성공한다() {
        assertThat(context.getLastResponse().jsonPath().getBoolean("success")).isTrue();
        assertThat(context.getLastResponse().jsonPath().getString("paymentKey")).isNotNull();
    }

    @그리고("결제 승인에 성공한다")
    public void 결제_승인에_성공한다() {
        assertThat(context.getLastResponse().jsonPath().getBoolean("success")).isTrue();
    }

    @그리고("상품이 {int}개 이상 포함되어 있다")
    public void 상품_개수_확인(int minCount) {
        int size = context.getLastResponse().jsonPath().getList("$").size();
        assertThat(size).isGreaterThanOrEqualTo(minCount);
    }
}
