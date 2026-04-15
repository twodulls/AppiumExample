package com.appium.utils;

import com.appium.common.ScreenshotListener;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;

/**
 * 클릭, 텍스트 입력 등 기본 액션
 */
public class ActionUtils {
    protected AndroidDriver driver;
    protected WebDriverWait wait;

    public ActionUtils(AndroidDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public static final String[] ALERT_POPUP_TEXTS = {
            "일시적인 오류",
            "네트워크 오류",
            "오류가 발생했습니다",
            "주문 마감시간이 지났습니다",
            "이미 장바구니에 담은 상품입니다"
    };
    private static final By CONFIRM_BTN = By.xpath("//android.widget.TextView[@text='확인']");

    /**
     * 기본 액션 (클릭, 입력, 조회)
     */
    public void click(WebElement element) {
        try {
            wait.until(ExpectedConditions.visibilityOf(element)).click();
        } catch (Exception e) {
            // 클릭 실패 시 알림 팝업 확인 후 재시도
            if (dismissAlertPopupIfPresent(ALERT_POPUP_TEXTS)) {
                wait.until(ExpectedConditions.visibilityOf(element)).click();
            } else {
                throw e;
            }
        }
    }

    // By 객체 전용 클릭 오버라이딩
    public void click(By locator) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).click();
        } catch (Exception e) {
            // 클릭 실패 시 알림 팝업 확인 후 재시도
            if (dismissAlertAndRetry()) {
                wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).click();
            } else {
                throw e;
            }
        }
    }

    /**
     * 알림 팝업이 떠있으면 스크린샷 첨부 후 '확인' 버튼을 클릭하여 닫습니다.
     * @return 팝업을 닫았으면 true, 팝업이 없으면 false
     */
    private boolean dismissAlertAndRetry() {
        WebDriverWait shortWait = new WebDriverWait(driver, java.time.Duration.ofSeconds(3));
        for (String popupText : ALERT_POPUP_TEXTS) {
            By popupTextBy = By.xpath(
                    String.format("//android.widget.TextView[contains(@text, '%s')]", popupText)
            );
            try {
                shortWait.until(ExpectedConditions.visibilityOfElementLocated(popupTextBy));

                // 팝업 감지 시 스크린샷 첨부
                try {
                    byte[] screenshot = driver
                            .getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
                    byte[] resized = ScreenshotListener.resizeScreenshot(screenshot, 0.3f);
                    io.qameta.allure.Allure.addAttachment(
                            "알림 팝업 감지 - " + popupText,
                            "image/png",
                            new java.io.ByteArrayInputStream(resized),
                            "png"
                    );
                } catch (Exception ignored) {}

                // '확인' 버튼 클릭하여 팝업 닫기
                shortWait.until(ExpectedConditions.elementToBeClickable(CONFIRM_BTN)).click();
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                return true;
            } catch (Exception ignored) {
                // 해당 텍스트의 팝업이 없는 경우 — 다음 텍스트 확인
            }
        }
        return false;
    }

    // 텍스트 입력
    public void sendKeys(WebElement element, String text) {
        wait.until(ExpectedConditions.visibilityOf(element));
        element.clear();
        element.sendKeys(text);
    }

    // 요소 표시 여부 대기 및 확인
    public boolean isDisplayed(WebElement element) {
        try {
            return wait.until(ExpectedConditions.visibilityOf(element)).isDisplayed();
        } catch (Exception e) {
            // 요소를 찾지 못했을 때 팝업이 떠있는지 확인 후 닫기
            if (dismissAlertAndRetry()) {
                try {
                    return wait.until(ExpectedConditions.visibilityOf(element)).isDisplayed();
                } catch (Exception ignored) {}
            }
            return false;
        }
    }

    // By 객체 전용 요소 표시 여부 대기 및 확인
    public boolean isDisplayed(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
        } catch (Exception e) {
            // 요소를 찾지 못했을 때 팝업이 떠있는지 확인 후 닫기
            if (dismissAlertAndRetry()) {
                try {
                    return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
                } catch (Exception ignored) {}
            }
            return false;
        }
    }

    // 특정 텍스트를 포함하는 요소를 반환합니다.
    public WebElement getElementByText(String text) {
        String xpath = "//*[contains(@text, '" + text + "')]";
        return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
    }

    // 요소에서 텍스트 추출하기
    public String getText(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element)).getText().trim();
    }

    /**
     * 화면에 특정 텍스트가 포함된 알림 팝업이 노출되면 스크린샷을 찍고 '확인' 버튼을 클릭하여 닫습니다.
     * @param popupTexts 감지할 팝업 텍스트 배열
     * @return 팝업을 닫았으면 true, 팝업이 없었으면 false
     */
    public boolean dismissAlertPopupIfPresent(String... popupTexts) {
        WebDriverWait microWait = new WebDriverWait(driver, java.time.Duration.ofMillis(1000));

        for (String popupText : popupTexts) {
            By popupTextBy = By.xpath(
                    String.format("//android.widget.TextView[contains(@text, '%s')]", popupText)
            );

            try {
                microWait.until(ExpectedConditions.visibilityOfElementLocated(popupTextBy));

                // 팝업 감지 시 로깅 및 스크린샷
                System.out.println(">>> [ActionUtils] 오류 팝업 감지: '" + popupText + "'");
                try {
                    byte[] screenshot = driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
                    byte[] resized = ScreenshotListener.resizeScreenshot(screenshot, 0.3f);
                    io.qameta.allure.Allure.addAttachment(
                            "오류 팝업 감지 - " + popupText, "image/png",
                            new java.io.ByteArrayInputStream(resized), "png"
                    );
                } catch (Exception ignored) {}

                // '확인' 버튼 클릭하여 팝업 닫기
                microWait.until(ExpectedConditions.elementToBeClickable(CONFIRM_BTN)).click();
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

                return true; // 팝업 하나 처리 후 즉시 true 반환
            } catch (Exception ignored) {
                // 해당 텍스트의 팝업이 없는 경우 — 다음 텍스트 확인
            }
        }
        return false;
    }

    // Android 키패드 전용 헬퍼 메소드: 숫자 문자를 안드로이드 네이티브 키 이벤트(AndroidKey)로 변환합니다.
    public static AndroidKey getAndroidKeyForChar(char c) {
        return switch (c) {
            case '0' -> AndroidKey.DIGIT_0;
            case '1' -> AndroidKey.DIGIT_1;
            case '2' -> AndroidKey.DIGIT_2;
            case '3' -> AndroidKey.DIGIT_3;
            case '4' -> AndroidKey.DIGIT_4;
            case '5' -> AndroidKey.DIGIT_5;
            case '6' -> AndroidKey.DIGIT_6;
            case '7' -> AndroidKey.DIGIT_7;
            case '8' -> AndroidKey.DIGIT_8;
            case '9' -> AndroidKey.DIGIT_9;
            default -> null; // 숫자 외에는 무시
        };
    }

    /**
     * 특정 요소가 하단 플로팅 바(또는 상단 헤더)에 가려지지 않는 '안전 영역'에 올 때까지 스크롤합니다.
     * @param driver AndroidDriver 인스턴스
     * @param element 보이게 만들고자 하는 대상 요소
     */
    public static void scrollIntoSafeZone(AndroidDriver driver, WebElement element) {
        int maxSwipes = 5; // 무한 스크롤 방지용 최대 횟수
        int screenHeight = driver.manage().window().getSize().getHeight();
        int screenWidth = driver.manage().window().getSize().getWidth();

        // 요소가 70% 위치보다 아래에 있으면 위로 스크롤합니다.
        int topSafeZone = (int) (screenHeight * 0.20);
        int bottomSafeZone = (int) (screenHeight * 0.70);
        int previousY = -1; // 이전 Y 좌표를 기억하기 위한 변수 (화면 끝 도달 확인용)

        for (int i = 0; i < maxSwipes; i++) {
            try {
                int elementY = element.getLocation().getY();
                if (elementY == previousY) {break;} // 화면 맨 아래 도달하여 스크롤 동작 정지
                previousY = elementY; // 현재 좌표를 이전 좌표로 저장

                if (elementY > bottomSafeZone) {
                    // 요소가 하단에 있음 -> 위로 쓸어올림 (Swipe Up)
                    swipe(driver, screenWidth / 2, (int) (screenHeight * 0.7), screenWidth / 2, (int) (screenHeight * 0.3));
                } else if (elementY < topSafeZone) {
                    // 요소가 상단에 있음 -> 아래로 쓸어내림 (Swipe Down)
                    swipe(driver, screenWidth / 2, (int) (screenHeight * 0.3), screenWidth / 2, (int) (screenHeight * 0.7));
                } else {
                    break; // 요소가 안전 영역에 도달하여 스크롤 동작 정지
                }
                Thread.sleep(1000); // 스크롤 후 UI가 안정화될 시간을 짧게 줍니다.

            } catch (Exception e) {
                break;
            }
        }
    }

    // 지정한 좌표로 드래그(스와이프)를 수행하는 내부 헬퍼 메서드
    private static void swipe(AndroidDriver driver, int startX, int startY, int endX, int endY) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        // 0.6초 동안 부드럽게 스크롤
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), endX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));
    }
}