package com.appium.page;

import com.appium.common.BasePage;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

public class HomePage extends BasePage {

    @AndroidFindBy(accessibility = "App 로고")
    @iOSXCUITFindBy(accessibility = "App 로고")
    private WebElement appLogo;

    public HomePage(AppiumDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public boolean isAppLogoDisplayed() { return isDisplayed(appLogo); }
}