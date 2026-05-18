package com.appium.common;

import com.appium.utils.ActionUtils;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BasePage extends ActionUtils {

    /**
     * BasePage 생성자
     * AppiumFieldDecorator를 사용하여 최대 10초의 암묵적 대기를 갖는 페이지 요소를 초기화합니다.
     * @param driver Appium WebDriver 인스턴스
     * @param wait   명시적 대기(Explicit Wait)를 위한 WebDriverWait 인스턴스
     */
    public BasePage(AppiumDriver driver, WebDriverWait wait) {
        super(driver, wait);
        PageFactory.initElements(new AppiumFieldDecorator(driver, java.time.Duration.ofSeconds(10)), this);
    }

    /**
     * 기본 액션 (클릭, 입력, 노출 확인 등, 일부는 ActionUtils에 상속됨)
     */

    /**
     * 이미 찾아진 WebElement의 노출 여부를 지정된 시간 동안 확인합니다.
     * @param element 노출 여부를 확인할 WebElement
     * @param seconds 최대 대기 시간 (초)
     * @return 지정된 시간 내에 화면에 노출되면 true, 아니면 false
     */
    public boolean isDisplayedWithTimeout(WebElement element, int seconds) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, java.time.Duration.ofSeconds(seconds));
            return shortWait.until(ExpectedConditions.visibilityOf(element)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * By 로케이터를 사용하여 동적 UI 요소가 지정된 시간 내에 노출되는지 안전하게 확인합니다.
     * @param locator 노출 여부를 확인할 대상의 By 객체
     * @param seconds 최대 대기 시간 (초)
     * @return 지정된 시간 내에 화면에 노출되면 true, 아니면 false
     */
    public boolean isDisplayedWithTimeout(org.openqa.selenium.By locator, int seconds) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, java.time.Duration.ofSeconds(seconds));
            return shortWait.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 대기 및 네비게이션 (로딩 대기)
     */
    /**
     * 화면에 '상품을 불러오고 있어요' 등 시스템 로딩 인디케이터가 표시 중일 경우, 완전히 사라질 때까지 대기합니다.
     * 로딩 화면이 나타나기까지의 찰나의 지연을 보완하기 위해 최초 1초 대기 후 검증을 시작합니다.
     */
    public void waitForLoadingToFinish() {
        By loadingTxtBy = BaseTest.platform.byLoadingText("불러오고 있어요");

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(loadingTxtBy));
        } catch (Exception e) {
            log.warn(">>> [BasePage] 로딩 화면 대기 중 타임아웃 발생");
        }
    }

    /**
     * 탭 제어 (GNB / Bottom Tab)
     */
    /**
     * 하단 탭(GNB)의 이름을 기반으로 플랫폼(Android/iOS)에 맞는 동적 로케이터(By)를 생성합니다.
     * @param tabName 찾고자 하는 탭의 텍스트명
     * @return 플랫폼에 맞게 생성된 By 로케이터
     */
    private By getTabLocator(String tabName) {
        return switch (BaseTest.platform) {
            case IOS     -> By.xpath(String.format(
                    "//XCUIElementTypeButton[contains(@name, '%1$s')] | //XCUIElementTypeLink[contains(@name, '%1$s')]",
                    tabName));
            case ANDROID -> BaseTest.platform.byText(tabName);
        };
    }

    /**
     * 지정된 이름의 하단 탭(전용몰, 경영, 리뷰, 본부소통, MY)을 클릭하여 화면을 이동합니다.
     * 전환 간의 렌더링 애니메이션 이슈를 방지하기 위해 클릭 전후로 짧은 대기 시간을 갖습니다.
     * @param tabName 클릭할 탭의 텍스트명
     */
    public void clickTab(String tabName) {
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        click(getTabLocator(tabName));
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        waitForLoadingToFinish();
    }

    /**
     * 특정 탭으로 이동했다가 다시 원래 탭으로 복귀하여 페이지 데이터(API)를 강제로 갱신합니다.
     * @param intermediateTab 갱신을 위해 임시로 다녀올 탭의 이름
     * @param targetTab       최종적으로 복귀하여 데이터를 갱신할 대상 탭의 이름
     */
    public void refreshPageByTabSwitching(String intermediateTab, String targetTab) {
        log.debug(">>> [BasePage] 데이터 갱신을 위해 탭 스위칭 수행: {} -> {}", intermediateTab, targetTab);
        clickTab(intermediateTab);
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        clickTab(targetTab);
    }

    /**
     * 단일 탭의 노출 여부를 확인합니다.
     * @param tabName 확인할 탭의 텍스트명
     * @return 화면에 탭이 존재하면 true
     */
    public boolean isTabDisplayed(String tabName) {
        return isDisplayed(getTabLocator(tabName));
    }

    /**
     * 유틸리티 (텍스트 추출, 다중 조건 클릭, 팝업 닫기)
     */

    /**
     * 첫 번째 대상(el1)의 노출을 짧게(3초) 대기한 후 클릭하며, 실패 시 두 번째 대상(el2)을 클릭(Fallback)합니다.
     * @param el1 우선적으로 클릭을 시도할 주 대상 요소
     * @param el2 첫 번째 요소가 없을 경우 클릭할 대체 요소
     */
    public void clickFirstVisible(WebElement el1, WebElement el2) {
        try {
            // 기본 wait(20초 등)를 쓰지 않고 3초만 확인하는 로직으로 교체
            WebDriverWait shortWait = new WebDriverWait(driver, java.time.Duration.ofSeconds(3));
            shortWait.until(ExpectedConditions.visibilityOf(el1)).click();
        } catch (Exception e) {
            // el1이 없으면 즉시 el2 클릭
            click(el2);
        }
    }

    /**
     * 다수의 WebElement 리스트에서 텍스트 속성만 순수하게 추출하여 String 리스트로 반환합니다.
     * @param elements 텍스트를 추출할 대상 요소 리스트
     * @return 공백이 제거된 텍스트 리스트 (빈 문자열 제외, 추출 실패 시 빈 리스트 반환)
     */
    protected List<String> getTextsFromElements(List<WebElement> elements) {
        try {
            return elements.stream()
                    .map(el -> el.getText().trim())
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn(">>> [BasePage] 요소에서 텍스트를 추출할 수 없습니다. (빈 리스트 반환)");
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Android 디바이스 전용 하드웨어 물리 키(Back, Home, Enter 등) 이벤트를 발생시킵니다.
     * @param key 전송할 AndroidKey 상수 (예: AndroidKey.BACK)
     */
    public void pressAndroidKey(AndroidKey key) {
        if (!BaseTest.platform.isAndroid()) {
            log.warn(">>> [BasePage] pressAndroidKey는 Android 전용 메서드입니다. iOS는 무시됩니다.");
            return;
        }
        ((io.appium.java_client.android.AndroidDriver) driver).pressKey(new KeyEvent(key));
    }
}