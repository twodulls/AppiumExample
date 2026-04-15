package com.appium.page;

import com.appium.common.BasePage;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage extends BasePage {

    // 아이디 입력란
    @AndroidFindBy(uiAutomator = "new UiSelector().className(\"android.widget.EditText\").instance(0)")
    private WebElement idField;

    // 패스워드 입력란
    @AndroidFindBy(uiAutomator = "new UiSelector().className(\"android.widget.EditText\").instance(1)")
    private WebElement pwField;

    // 로그인 버튼
    @AndroidFindBy(xpath = "//android.widget.Button[@text=\"로그인\"]")
    private WebElement loginBtn;

    // 로그인 실패 안내 텍스트
    @AndroidFindBy(uiAutomator = "new UiSelector().text(\"아이디 또는 패스워드가 일치하지 않습니다.\")")
    private WebElement loginFailedTxt;

    public LoginPage(AndroidDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    // 아이디, 비밀번호 입력 후 로그인 시도
    public void login(String id, String pw) {
        sendKeys(idField, id);
        sendKeys(pwField, pw);
        click(loginBtn);
    }

    // 로그인 버튼 노출 여부 확인
    public boolean isLoginBtnDisplayed() {
        return isDisplayedWithTimeout(loginBtn,3);
    }

    // 로그인 실패 텍스트 노출 여부 확인
    public boolean isLoginFailedTxtDisplayed() {
        return isDisplayedWithTimeout(loginFailedTxt, 5);
    }
}