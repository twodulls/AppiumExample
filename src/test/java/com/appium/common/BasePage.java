package com.appium.common;

import com.appium.utils.ActionUtils;
import io.appium.java_client.android.AndroidDriver;
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
     * 변수 및 생성자
     */
    private static final String[] BOTTOM_TABS = {"A", "B", "C", "D", "E"};

    public BasePage(AndroidDriver driver, WebDriverWait wait) {
        super(driver, wait); // ActionUtils 생성자 호출
        PageFactory.initElements(new AppiumFieldDecorator(driver), this);
    }

    /**
     * 기본 액션 (클릭, 입력, 노출 확인 등, 일부는 ActionUtils에 상속됨)
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
     * By 객체 전용: 특정 요소가 지정된 시간 내에 보이는지 확인 (동적 UI용)
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
    // 화면에 'Loading' 등의 로딩 UI가 표시 중이라면, 완전히 사라질 때까지 대기합니다.
    public void waitForLoadingToFinish() {
        By loadingTxtBy = By.xpath("//android.widget.TextView[contains(@text, 'Loading')]");
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
    // accessibilityId가 있는 전용몰/경영/리뷰/공지/MY 탭만 클릭 가능
    public void clickTab(String tabName) {
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        String tabXpath = String.format("//android.widget.TextView[@text='%s']/parent::android.view.View", tabName);
        click(By.xpath(tabXpath));

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        waitForLoadingToFinish();
    }

    /**
     * 현재 페이지의 데이터 갱신을 위해 다른 탭으로 이동했다가 다시 복귀합니다.
     * @param intermediateTab 갱신을 위해 이동할 탭
     * @param targetTab 복귀할 탭
     */
    public void refreshPageByTabSwitching(String intermediateTab, String targetTab) {
        log.debug(">>> [BasePage] 데이터 갱신을 위해 탭 스위칭 수행: {} -> {}", intermediateTab, targetTab);
        clickTab(intermediateTab);
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        clickTab(targetTab);
    }

    public boolean isTabDisplayed(String tabName) {
        String tabXpath = String.format("//android.widget.TextView[@text='%s']/parent::android.view.View", tabName);
        return isDisplayed(By.xpath(tabXpath));
    }

    public boolean isAllTabsDisplayed() {
        for (String tabName : BOTTOM_TABS) {
            if (!isTabDisplayed(tabName)) {
                log.warn(">>> [BasePage] 하단 탭 미노출: {}", tabName);
                return false;
            }
        }
        return true;
    }

    /**
     * 유틸리티 (텍스트 추출, 다중 조건 클릭, 팝업 닫기)
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
     * 다수의 WebElement 리스트에서 텍스트만 추출하여 String 리스트로 반환합니다.
     */
    protected List<String> getTextsFromElements(List<WebElement> elements) {
        try {
            wait.until(ExpectedConditions.visibilityOfAllElements(elements));
            return elements.stream()
                    .map(el -> el.getText().trim())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn(">>> [BasePage] 요소에서 텍스트를 추출할 수 없습니다. (빈 리스트 반환)");
            return java.util.Collections.emptyList();
        }
    }
}