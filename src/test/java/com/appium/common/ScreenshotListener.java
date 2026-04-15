package com.appium.common;

import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;

@Slf4j
public class ScreenshotListener implements ITestListener {

    // 테스트 실패 시 스크린샷 첨부
    @Override
    public void onTestFailure(ITestResult result) {
        String screenshotName = buildScreenshotName(result);
        attachScreenshot(screenshotName);
    }

    // 테스트 스킵 시 스크린샷 첨부
    @Override
    public void onTestSkipped(ITestResult result) {
        attachScreenshot("스킵 스크린샷");
    }

    /**
     * 재시도 횟수를 포함한 스크린샷 이름을 생성합니다.
     * 예: "실패 스크린샷 - 1회차", "실패 스크린샷 - 2회차", "실패 스크린샷 - 3회차 (최종)"
     */
    private String buildScreenshotName(ITestResult result) {
        try {
            RetryAnalyzer retryAnalyzer = (RetryAnalyzer) result.getMethod()
                    .getRetryAnalyzer(result);
            if (retryAnalyzer != null) {
                int currentRetry = retryAnalyzer.getRetryCount();
                if (currentRetry == 0) {
                    return "실패" + " 스크린샷 - 1회차";
                } else if (currentRetry < RetryAnalyzer.MAX_RETRY_COUNT) {
                    return "실패" + " 스크린샷 - " + (currentRetry + 1) + "회차";
                } else {
                    return "실패" + " 스크린샷 - " + (currentRetry + 1) + "회차 (최종)";
                }
            }
        } catch (Exception e) {
            log.warn(">>> [ScreenshotListener] 재시도 횟수 조회 실패: {}", e.getMessage());
        }
        return "실패" + " 스크린샷";
    }

    private void attachScreenshot(String screenshotName) {
        try {
            if (BaseTest.driver == null) {
                log.warn(">>> [ScreenshotListener] driver가 null이어서 스크린샷을 촬영할 수 없습니다.");
                return;
            }
            byte[] screenshot = BaseTest.driver.getScreenshotAs(OutputType.BYTES);
            byte[] resized = resizeScreenshot(screenshot, 0.3f);
            Allure.addAttachment(screenshotName, "image/png", new ByteArrayInputStream(resized), "png");
            log.debug(">>> [ScreenshotListener] Allure 리포트에 스크린샷 첨부 완료: {}", screenshotName);
        } catch (Exception e) {
            log.warn(">>> [ScreenshotListener] 스크린샷 첨부 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 스크린샷 이미지를 지정된 비율로 리사이즈합니다.
     * @param originalBytes 원본 이미지 바이트 배열
     * @param scale 축소 비율 (예: 0.5f = 50% 축소)
     * @return 리사이즈된 이미지 바이트 배열
     */
    public static byte[] resizeScreenshot(byte[] originalBytes, float scale) {
        try {
            java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(
                    new ByteArrayInputStream(originalBytes));
            int newWidth = (int) (original.getWidth() * scale);
            int newHeight = (int) (original.getHeight() * scale);

            java.awt.image.BufferedImage resized = new java.awt.image.BufferedImage(
                    newWidth, newHeight, original.getType());
            java.awt.Graphics2D g = resized.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, newWidth, newHeight, null);
            g.dispose();

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(resized, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            return originalBytes; // 리사이즈 실패 시 원본 반환
        }
    }
}
