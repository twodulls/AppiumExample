package com.appium.common;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

@Getter
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(RetryAnalyzer.class);
    public static final int MAX_RETRY_COUNT = 2;
    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY_COUNT) {
            // 재시도 전 드라이버 세션이 유효한지 확인
            if (!isDriverSessionValid()) {
                log.warn(">>> [Retry] '{}' 드라이버 세션이 유효하지 않아 재시도를 중단합니다.", result.getName());
                return false;
            }
            retryCount++;
            log.warn(">>> [Retry] '{}' 테스트 실패 - {}번째 재시도 중... (최대 {}회)",
                    result.getName(), retryCount, MAX_RETRY_COUNT);
            return true;
        }
        log.error(">>> [Retry] '{}' 테스트 최종 실패 - {}회 재시도 모두 실패",
                result.getName(), MAX_RETRY_COUNT);
        return false;
    }

    /**
     * 드라이버 세션이 현재 유효한 상태인지 확인합니다.
     * 세션이 종료되었거나 null인 경우 false를 반환합니다.
     */
    private boolean isDriverSessionValid() {
        try {
            if (BaseTest.driver == null) return false;
            // 세션 ID가 존재하는지로 유효성 판단
            return BaseTest.driver.getSessionId() != null;
        } catch (Exception e) {
            log.warn(">>> [Retry] 드라이버 세션 유효성 확인 중 오류: {}", e.getMessage());
            return false;
        }
    }
}
