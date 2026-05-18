package com.appium.uitest;

import com.appium.common.BaseTest;
import com.appium.page.HomePage;
import com.appium.page.LoginPage;
import io.qameta.allure.*;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static io.qameta.allure.Allure.step;

@Epic("Main 화면")
@Feature("Login 기능")
@Slf4j
public class LoginTest extends BaseTest {
    private LoginPage loginPage;
    private HomePage homePage;
    private SoftAssertions sa;

    // LoginTest는 BaseTest의 자동 로그인을 실행하지않음
    @Override
    protected boolean isLoginRequired() {
        return false;
    }

    @BeforeClass
    public void beforeClass() {
        loginPage = new LoginPage(driver, wait);
        homePage = new HomePage(driver, wait);
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(){ sa = new SoftAssertions(); }

    @Severity(SeverityLevel.NORMAL)
    @Test(priority = 1)
    public void 유효하지않은_계정_로그인_불가_동작_확인() {
        step("[Given] 현재 로그인 상태라면 마이페이지로 이동하여 로그아웃 수행");
        if (!loginPage.isLoginBtnDisplayed()) {
            log.info(">>> [LoginTest] 이전 테스트로 인해 로그인 상태입니다. 로그아웃을 진행합니다.");
            homePage.clickTab("MY");
        }

        step("[When] 유효하지 않은 계정으로 로그인 시도");
        loginPage.login("qa_test_not_exist_999", "QaTest!@#1234");

        step("[Then] 로그인 실패 에러 메시지 노출 검증");
        sa.assertThat(loginPage.isLoginFailedTxtDisplayed())
                .as("로그인 실패 에러 메시지가 노출되지 않았습니다.")
                .isTrue();

        sa.assertAll();
    }

    @Severity(SeverityLevel.BLOCKER)
    @Test(priority = 2)
    public void 테스트_계정_로그인_및_홈화면_진입_확인() {
        step("[Given] 현재 로그인 상태라면 마이페이지로 이동하여 로그아웃 수행");
        if (!loginPage.isLoginBtnDisplayed()) {
            log.info(">>> [LoginTest] 이전 테스트로 인해 로그인 상태입니다. 로그아웃을 진행합니다.");
            homePage.clickTab("MY");
        }

        step("[When] 유효한 계정으로 로그인 시도");
        loginPage.login(ID, PW);

        step("[Then] 로그인 성공 후 홈 화면의 주요 UI 요소(앱 로고, 하단 탭)가 정상 노출되는지 검증");
        boolean isLogoVisible = homePage.isAppLogoDisplayed();
        log.debug(">>> [Verify] 홈 화면 앱 로고 노출 여부: {}", isLogoVisible);

        sa.assertThat(isLogoVisible)
                .as("로그인 성공 후 홈 화면에 앱 로고가 노출되지 않았습니다.")
                .isTrue();

        sa.assertAll();
    }
}
