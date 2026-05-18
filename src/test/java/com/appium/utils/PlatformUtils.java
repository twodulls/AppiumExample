package com.appium.utils;

import org.openqa.selenium.By;

// 지원하는 모바일 플랫폼을 정의하고, 플랫폼별 XPath 생성 유틸리티를 제공합니다.
public enum PlatformUtils {
    ANDROID, IOS;

    /**
     * 플랫폼 초기화
     */

    // 시스템 프로퍼티(-Dplatform)에서 플랫폼을 읽어 반환합니다. android/ios 외의 값이 들어오면 명확한 에러를 발생시킵니다.
    public static PlatformUtils fromSystemProperty() {
        String value = System.getProperty("platform");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "-Dplatform 값이 지정되지 않았습니다. android 또는 ios 중 하나를 입력해주세요.\n" +
                    "예: ./gradlew testAndroid allureReport_android --continue\n" +
                    "예: ./gradlew testIos allureReport_ios --continue");
        }
        return switch (value.toLowerCase().trim()) {
            case "android" -> ANDROID;
            case "ios"     -> IOS;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 플랫폼입니다: '" + value + "'. android 또는 ios 중 하나를 입력해주세요.");
        };
    }

    public boolean isAndroid() { return this == ANDROID; }
    public boolean isIos()     { return this == IOS; }

    /**
     * XPath 로케이터 생성 유틸리티
     */

    /**
     * 텍스트가 정확히 일치하는 요소를 찾는 By 로케이터를 반환합니다.
     * <p>Android: @text='X'</p>
     * <p>iOS: @name='X' or @label='X' </p>
     */
    public By byText(String text) {
        return switch (this) {
            case ANDROID -> By.xpath(String.format("//*[@text='%s']", text));
            case IOS     -> By.xpath(String.format("//*[@name='%1$s' or @label='%1$s']", text));
        };
    }

    /**
     * 텍스트를 포함하는 요소를 찾는 By 로케이터를 반환합니다.
     * <p>Android: contains(@text,'X')</p>
     * <p>iOS: contains(@label,'X') or contains(@name,'X')</p>
     */
    public By byContainsText(String text) {
        return switch (this) {
            case ANDROID -> By.xpath(String.format("//*[contains(@text, '%s')]", text));
            case IOS     -> By.xpath(String.format("//*[contains(@label, '%1$s') or contains(@name, '%1$s')]", text));
        };
    }

    /**
     * 텍스트가 정확히 일치하는 팝업 텍스트 요소를 찾는 By 로케이터를 반환합니다.
     * <p>Android: android.widget.TextView</p>
     * <p>iOS: XCUIElementTypeStaticText</p>
     */
    public By byPopupText(String text) {
        return switch (this) {
            case ANDROID -> By.xpath(String.format("//android.widget.TextView[contains(@text, '%s')]", text));
            case IOS     -> By.xpath(String.format("//XCUIElementTypeStaticText[contains(@label, '%s')]", text));
        };
    }

    // 상품 체크박스 전체 리스트를 찾는 XPath 문자열을 반환합니다.
    public String itemCheckboxXpath() {
        return switch (this) {
            case ANDROID -> "//android.widget.CheckBox[@text='']";
            case IOS     -> "//XCUIElementTypeSwitch[not(@name='전체 선택')]";
        };
    }

    /**
     * 요소의 텍스트 속성명을 반환합니다.
     * <p>Android: "text"</p>
     * <p>iOS: "name"</p>
     */
    public String textAttr() {
        return switch (this) {
            case ANDROID -> "text";
            case IOS     -> "name";
        };
    }

    // 로딩 중 텍스트를 찾는 By 로케이터를 반환합니다.
    public By byLoadingText(String text) {
        return switch (this) {
            case ANDROID -> By.xpath(String.format("//android.widget.TextView[contains(@text, '%s')]", text));
            case IOS     -> By.xpath(String.format("//XCUIElementTypeStaticText[contains(@label, '%s')]", text));
        };
    }

    // 팝업 확인 버튼 로케이터를 반환합니다.
    public By confirmBtnLocator() {
        return switch (this) {
            case ANDROID -> By.xpath("//android.widget.TextView[@text='확인']");
            case IOS     -> By.xpath("//XCUIElementTypeButton[@name='확인' or @label='확인']");
        };
    }
}