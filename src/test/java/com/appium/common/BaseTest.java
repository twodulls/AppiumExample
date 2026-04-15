package com.appium.common;

import com.appium.page.HomePage;
import com.appium.page.LoginPage;
import com.appium.testdata.TestData;
import com.appium.testdata.TestDataManager;
import com.appium.utils.ActionUtils;
import com.google.common.collect.ImmutableMap;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import static com.github.automatedowl.tools.AllureEnvironmentWriter.allureEnvironmentWriter;

@Slf4j
public abstract class BaseTest {
    public static AppiumDriverLocalService service;
    public static AndroidDriver driver;
    public static WebDriverWait wait;
    public BasePage basePage;

    public static String ENV;
    public static TestData testData;
    public static String ID, PW;

    /**
     * 기본적으로 모든 테스트는 자동 로그인이 필요하다고 설정합니다.
     * 로그인이 필요 없는 클래스(예: LoginTest)에서만 이 메서드를 오버라이드하여 false를 반환합니다.
     */
    protected boolean isLoginRequired() {
        return true;
    }

    // TestNG LifeCycle (Suite -> Test -> Class -> Method)
    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        String env = System.getProperty("env");
        // dev 혹은 it 환경으로 테스트 수행하기 위해서는 env(yml) 파일을 환경에 맞게 세팅해야 한다.
        if (env == null) env = "qa";
        log.debug(">>> [BaseTest] env: : {}", env);

        ENV = env;
        testData = TestDataManager.getInstance(env).getData();
        ID = testData.getId();
        PW = testData.getPw();
    }

    @BeforeTest(alwaysRun = true)
    public void beforeTest(ITestContext ctx) throws MalformedURLException {
        //suite xml 에서 suite name 을 읽어옴 - Allure Report에 suite name을 넣기 위해
        String suiteName = ctx.getCurrentXmlTest().getSuite().getName();
        log.debug(">>> [BaseTest] suite name : {}", suiteName);

        // Allure 리포트 환경 정보
        String appVersion = AdbDevices.getAppVersion(testData.getAppPackage());
        String deviceManufacturer = AdbDevices.getDeviceManufacturer();
        String deviceModel = AdbDevices.getDeviceModel();
        String deviceOsVersion = AdbDevices.getDeviceOsVersion();
        log.debug(">>> [BaseTest] 앱 버전: {} / 기기: {} {} (Android {})",
                appVersion, deviceManufacturer, deviceModel, deviceOsVersion);

        allureEnvironmentWriter(
                ImmutableMap.<String, String>builder()
                        .put("Environment", ENV)
                        .put("Suite Name", suiteName)
                        .put("App Package", testData.getAppPackage())
                        .put("App Version", appVersion)
                        .put("Device Manufacturer", deviceManufacturer)
                        .put("Device Model", deviceModel)
                        .put("Android OS", deviceOsVersion)
                        .build(),
                System.getProperty("user.dir") + "/build/allure-results/");

        // appium service start
        AppiumServiceBuilder serviceBuilder = new AppiumServiceBuilder();
        serviceBuilder.withIPAddress(testData.getIpAddress())
                .usingPort(testData.getPort())
                .withArgument(() -> "--allow-insecure", "*:chromedriver_autodownload")
                .withArgument(GeneralServerFlag.LOG_LEVEL, "error");

        service = AppiumDriverLocalService.buildService(serviceBuilder);
        service.start();

        if(!service.isRunning()){
            throw new RuntimeException("appium server is not started..");
        }

        //adb device로 udid 가져오기
        List<String> udids = AdbDevices.getAdbDevices();
        if (udids.isEmpty()) {
            log.debug(">>> [BaseTest] 연결된 기기가 없거나 ADB가 설정되지 않았습니다.");
        } else {
            for (String udid : udids) {
                log.debug(">>> [BaseTest] UDID: {}", udid);
            }
        }

        String deviceUDID = udids.getFirst();

        driver = new AndroidDriver(new URL("http://"+testData.getIpAddress() + ":" + testData.getPort()), setOption(deviceUDID));   //http://127.0.0.1:4723
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));  // 명시적 대기

        //페이지 초기화
        basePage = new BasePage(driver, wait);
        if (isLoginRequired()) {
            performGlobalLogin(); // 로그아웃 상태라면, 로그인 진행
        } else {
            log.info(">>> [BaseTest] 이 테스트 클래스는 자동 로그인을 건너뜁니다.");
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeMethodBase() {
        // 모든 테스트 실행 전 알림 팝업이 떠있으면 자동으로 닫습니다.
        basePage.dismissAlertPopupIfPresent(ActionUtils.ALERT_POPUP_TEXTS);
    }

    @AfterTest
    public void afterTest(){
        if(driver != null) {
            driver.quit();
            driver = null; // 초기화
        }
        if(service != null) service.stop();
        log.info(">>> [BaseTest] Appium 드라이버 및 서비스를 종료합니다.");
    }

    //Android (w3c-compliant)
    public UiAutomator2Options setOption(String udid){
        UiAutomator2Options options = new UiAutomator2Options();
        options.setUdid(udid);
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setAppPackage(testData.getAppPackage());
        options.setNoReset(true);
        options.setNativeWebScreenshot(true);
        options.setEnsureWebviewsHavePages(true);
        options.setAutoGrantPermissions(true);
        return options;
    }

    //iOS (w3c-compliant)
//    public XCUITestOptions setOption(){
//        XCUITestOptions options = new XCUITestOptions();
//        options.setPlatformName("iOS");
//        options.setAutomationName("XCUITest");
//
//        IOSDriver driver = new IOSDriver(new URL("http://localhost:4723"), options);
//        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(100));
//        return options;
//    }

    // 현재 로그인 상태를 확인하고, 로그인이 필요할 때만 1회 수행
    public void performGlobalLogin() {
        // 앱 스플래시 화면 대기
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        LoginPage loginPage = new LoginPage(driver, wait);
        HomePage homePage = new HomePage(driver, wait);
        boolean isLoginNeeded = loginPage.isLoginBtnDisplayed();
        if (isLoginNeeded) {
            log.info(">>> [Auth] 로그아웃 상태 확인됨. 자동 로그인을 수행합니다.");
            try {
                loginPage.login(ID, PW);
                wait.until(d -> homePage.isAppLogoDisplayed());
            } catch (Exception loginException) {
                loginException.printStackTrace();
                throw new RuntimeException("로그인 수행 실패", loginException);
            }
        } else {
            log.info(">>> [Auth] 이미 로그인된 상태입니다. 테스트를 바로 시작합니다.");
        }
    }
}
