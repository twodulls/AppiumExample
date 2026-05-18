package com.appium.common;

import com.appium.page.HomePage;
import com.appium.page.LoginPage;
import com.appium.testdata.TestData;
import com.appium.testdata.TestDataManager;
import com.appium.utils.ActionUtils;
import com.appium.utils.AdbDevices;
import com.appium.utils.IosDevices;
import com.appium.utils.PlatformUtils;
import com.google.common.collect.ImmutableMap;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static com.github.automatedowl.tools.AllureEnvironmentWriter.allureEnvironmentWriter;

@Slf4j
public abstract class BaseTest {
    public static String ENV;
    public static TestData testData;
    public static String ID, PW;

    public static AppiumDriverLocalService service;
    public static AppiumDriver driver; // Android/iOS 양쪽을 수용하는 부모 타입으로 통일
    public static PlatformUtils platform; // 현재 실행 플랫폼 (페이지 클래스에서도 참조 가능)
    public static WebDriverWait wait;
    public static BasePage basePage;

    /**
     * 기본적으로 모든 테스트는 자동 로그인이 필요하다고 설정합니다.
     * 로그인이 필요 없는 클래스(예: LoginTest)에서만 이 메서드를 오버라이드하여 false를 반환합니다.
     */
    protected boolean isLoginRequired() {
        return true;
    }

    /**
     * TestNG LifeCycle (Suite -> Test -> Class -> Method)
     */
    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        String env = System.getProperty("env");
        if (env == null) env = "qa";
        log.debug(">>> [BaseTest] env: : {}", env);

        ENV = env;
        testData = TestDataManager.getInstance(env).getData();
        ID = testData.getId();
        PW = testData.getPw();
    }

    @BeforeTest(alwaysRun = true)
    public void beforeTest(ITestContext ctx) throws MalformedURLException {
        String suiteName = ctx.getCurrentXmlTest().getSuite().getName();
        log.debug(">>> [BaseTest] suite name : {}", suiteName);

        // 플랫폼 파라미터 읽기 (-Dplatform=android 또는 -Dplatform=ios, 기본값: android)
        platform = PlatformUtils.fromSystemProperty();
        log.debug(">>> [BaseTest] 실행 플랫폼: {}", platform);

        // Appium 서비스 시작
        AppiumServiceBuilder serviceBuilder = new AppiumServiceBuilder();
        serviceBuilder.withIPAddress(testData.getIpAddress())
                .usingPort(testData.getPort())
                .withArgument(() -> "--allow-insecure", "*:chromedriver_autodownload")
                .withArgument(GeneralServerFlag.LOG_LEVEL, "error");

        service = AppiumDriverLocalService.buildService(serviceBuilder);
        service.clearOutPutStreams();
        service.start();

        if (!service.isRunning()) {
            throw new RuntimeException("Appium 서버가 시작되지 않았습니다.");
        }

        URL appiumUrl = service.getUrl();

        // 플랫폼에 따라 드라이버 분기 생성 (android/ios 외의 값은 fromSystemProperty()에서 에러 발생)
        switch (platform) {
            case ANDROID -> driver = new AndroidDriver(appiumUrl, setAndroidOptions(AdbDevices.getAdbDevices().getFirst()));
            case IOS     -> driver = new IOSDriver(appiumUrl, setIosOptions());
        }
        writeAllureEnvironment(suiteName);

        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        basePage = new BasePage(driver, wait);

        // 최초 설치 후, 로그인시 아이콘 변경 알림: "You have changed the icon for 'App Name'."
        // basePage.dismissSystemAlertIfPresent(); 방어로직 추가필요

        if (isLoginRequired()) {
            performGlobalLogin();
        } else {
            log.info(">>> [BaseTest] 이 테스트 클래스는 자동 로그인을 건너뜁니다.");
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeMethodBase() {
        // beforeTest 실패 시 basePage가 null일 수 있으므로 null 체크 후 실행
        if (basePage != null) {
            basePage.dismissAlertPopupIfPresent(ActionUtils.ALERT_POPUP_TEXTS);
        }
    }

    @AfterTest
    public void afterTest() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
        if (service != null) service.stop();
        log.info(">>> [BaseTest] Appium 드라이버 및 서비스를 종료합니다.");
    }

    /** Android 드라이버 옵션을 생성합니다. (UiAutomator2) */
    private UiAutomator2Options setAndroidOptions(String udid) {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setUdid(udid);
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setAppPackage(testData.getAndroid().getAppPackage());
        options.setNoReset(true);
        options.setNativeWebScreenshot(true);
        options.setEnsureWebviewsHavePages(true);
        options.setAutoGrantPermissions(true);
        return options;
    }

    /** iOS 드라이버 옵션을 생성합니다. (XCUITest) */
    private XCUITestOptions setIosOptions() {
        XCUITestOptions options = new XCUITestOptions();
        options.setUdid(testData.getIos().getUdid());
        options.setPlatformName("iOS");
        options.setAutomationName("XCUITest");
        options.setBundleId(testData.getIos().getBundleId());
        options.setCapability("appium:xcodeOrgId", testData.getIos().getXcodeOrgId());
        options.setCapability("appium:xcodeSigningId", "iPhone Developer");
        options.setCapability("appium:autoAcceptAlerts", true);
        options.setNoReset(true);
        options.setCapability("appium:updatedWDABundleId", "com.domain.apppackage.wda.runner");
        options.setCapability("appium:shouldUseSingletonTestManager", false);
        options.setCapability("appium:includeDeviceCapsToSessionInfo", false);
        options.setCapability("appium:defaultAlertAction", "accept");
        return options;
    }

    private void writeAllureEnvironment(String suiteName) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
                .put("Environment", ENV)
                .put("Suite Name", suiteName)
                .put("Platform", platform.name());

        switch (platform) {
            case IOS -> {
                String udid = testData.getIos().getUdid();
                String appVersion  = IosDevices.getAppVersion(udid, testData.getIos().getBundleId());
                String deviceName  = IosDevices.getDeviceName(udid);
                String deviceModel = IosDevices.getDeviceModel(udid);
                String osVersion  = IosDevices.getOsVersion(udid);
                log.debug(">>> [BaseTest] 앱 버전: {} / iOS 기기정보 (OS): {} {} ({})",
                        appVersion, deviceName, deviceModel, osVersion);
                builder.put("Bundle ID",    testData.getIos().getBundleId())
                        .put("iOS UDID", udid)
                        .put("App Version", appVersion)
                        .put("Device Name", deviceName)
                        .put("Device Model", deviceModel)
                        .put("iOS OS Version",  osVersion);
            }
            case ANDROID -> {
                String appVersion = AdbDevices.getAppVersion(testData.getAndroid().getAppPackage());
                String deviceManufacturer = AdbDevices.getDeviceManufacturer();
                String deviceModel = AdbDevices.getDeviceModel();
                String osVersion = AdbDevices.getDeviceOsVersion();
                log.debug(">>> [BaseTest] 앱 버전: {} / Android 기기정보 (OS): {} {} ({})",
                        appVersion, deviceManufacturer, deviceModel, osVersion);
                builder.put("App Package", testData.getAndroid().getAppPackage())
                        .put("Android UDID", AdbDevices.getAdbDevices().getFirst())
                        .put("App Version", appVersion)
                        .put("Device Manufacturer", deviceManufacturer)
                        .put("Device Model", deviceModel)
                        .put("Android OS", osVersion);
            }
        }
        allureEnvironmentWriter(builder.build(),
                System.getProperty("allure.results.directory",
                        System.getProperty("user.dir") + "/build/allure-results") + "/");
    }

    /** 현재 로그인 상태를 확인하고, 로그인이 필요할 때만 1회 수행 */
    public void performGlobalLogin() {
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
