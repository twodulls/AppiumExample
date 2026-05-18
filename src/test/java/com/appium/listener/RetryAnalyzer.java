package com.appium.listener;

import com.appium.common.BaseTest;
import com.appium.utils.ActionUtils;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 테스트 재시도 분석기 및 전역 등록기를 통합한 클래스입니다.
 * IRetryAnalyzer: 테스트 실패 시 재시도 여부를 결정합니다.
 * IAnnotationTransformer: 모든 @Test 메서드에 RetryAnalyzer를 자동으로 주입합니다.
 * suite.xml의 listeners에 등록하여 사용합니다.
 */
@Getter
public class RetryAnalyzer implements IRetryAnalyzer, IAnnotationTransformer {

    private static final Logger log = LoggerFactory.getLogger(RetryAnalyzer.class);
    public static final int MAX_RETRY_COUNT = 2;
    private int retryCount = 0;

    // IAnnotationTransformer: 모든 @Test 메서드에 RetryAnalyzer 자동 주입
    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }

    // IRetryAnalyzer: 재시도 여부 결정
    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY_COUNT) {
            if (!isDriverSessionValid()) {
                log.warn(">>> [Retry] '{}' 드라이버 세션이 유효하지 않아 재시도를 중단합니다.", result.getName());
                return false;
            }

            // basePage 재사용 (ActionUtils 중복 구현 제거)
            // @BeforeMethod보다 먼저 선제적으로 팝업 처리
            try {
                if (BaseTest.basePage != null) {
                    boolean dismissed = BaseTest.basePage.dismissAlertPopupIfPresent(
                            ActionUtils.ALERT_POPUP_TEXTS);
                    if (dismissed) {
                        log.warn(">>> [Retry] 재시도 전 알림 팝업 처리 완료");
                    }
                }
            } catch (Exception e) {
                log.warn(">>> [Retry] 팝업 처리 중 오류 (무시하고 재시도 진행): {}", e.getMessage());
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
     */
    private boolean isDriverSessionValid() {
        try {
            if (BaseTest.driver == null) return false;
            return BaseTest.driver.getSessionId() != null;
        } catch (Exception e) {
            log.warn(">>> [Retry] 드라이버 세션 유효성 확인 중 오류: {}", e.getMessage());
            return false;
        }
    }
}
