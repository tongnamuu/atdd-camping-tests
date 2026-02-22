package com.camping.tests.support;

/**
 * 외부 환경 변수로부터 서비스 기본 URL을 읽어 제공한다.
 *
 * 환경 변수:
 *   KIOSK_BASE_URL    (기본값: http://localhost:8080)
 *   PAYMENTS_BASE_URL (기본값: http://localhost:9090)
 */
public class TestConfig {

    public static final String KIOSK_BASE_URL =
            System.getenv().getOrDefault("KIOSK_BASE_URL", "http://localhost:8080");

    public static final String PAYMENTS_BASE_URL =
            System.getenv().getOrDefault("PAYMENTS_BASE_URL", "http://localhost:9090");

    private TestConfig() {}
}
