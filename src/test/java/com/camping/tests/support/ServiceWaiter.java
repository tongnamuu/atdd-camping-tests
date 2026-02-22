package com.camping.tests.support;

import io.restassured.RestAssured;

/**
 * 서비스 준비 대기 유틸리티.
 * 지정한 URL에 GET 요청을 보내 HTTP 200이 돌아올 때까지 폴링한다.
 */
public class ServiceWaiter {

    /**
     * @param url        헬스 체크 URL
     * @param maxRetries 최대 재시도 횟수
     * @param intervalMs 재시도 간격 (ms)
     */
    public static void waitFor(String url, int maxRetries, long intervalMs) throws InterruptedException {
        System.out.printf("[ServiceWaiter] Waiting for %s (max %d retries, interval %dms)%n",
                url, maxRetries, intervalMs);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                int status = RestAssured.given()
                        .get(url)
                        .getStatusCode();
                if (status == 200) {
                    System.out.printf("[ServiceWaiter] %s is UP (attempt %d)%n", url, attempt);
                    return;
                }
                System.out.printf("[ServiceWaiter] Attempt %d/%d — HTTP %d%n", attempt, maxRetries, status);
            } catch (Exception e) {
                System.out.printf("[ServiceWaiter] Attempt %d/%d — not reachable (%s)%n",
                        attempt, maxRetries, e.getClass().getSimpleName());
            }
            Thread.sleep(intervalMs);
        }

        throw new IllegalStateException(
                String.format("[ServiceWaiter] %s did not become ready after %d attempts", url, maxRetries));
    }
}
