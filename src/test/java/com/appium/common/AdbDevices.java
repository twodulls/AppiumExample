package com.appium.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AdbDevices {

    public static List<String> getAdbDevices() {
        List<String> udids = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("adb devices");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith("\tdevice")) {
                    String udid = line.split("\t")[0];
                    udids.add(udid);
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return udids;
    }

    /**
     * ADB 명령어로 기기에 설치된 앱의 버전명을 가져옵니다.
     * @param appPackage 앱 패키지명 (예: com.sfn.oesikup)
     * @return 앱 버전명 (예: 1.0.0), 실패 시 "unknown"
     */
    public static String getAppVersion(String appPackage) {
        try {
            String[] command = {
                "adb", "shell", "dumpsys", "package", appPackage
            };
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("versionName")) {
                    String version = line.trim().split("=")[1].trim();
                    reader.close();
                    return version;
                }
            }
            reader.close();
        } catch (Exception e) {
            System.err.println(">>> [AdbDevices] 앱 버전을 가져오는데 실패했습니다: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * ADB 명령어로 기기 정보를 가져옵니다.
     * @param property 기기 속성명 (예: ro.product.model, ro.product.manufacturer, ro.build.version.release)
     * @return 기기 속성값, 실패 시 "unknown"
     */
    private static String getDeviceProperty(String property) {
        try {
            String[] command = {"adb", "shell", "getprop", property};
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String value = reader.readLine();
            reader.close();
            return (value != null && !value.isEmpty()) ? value.trim() : "unknown";
        } catch (Exception e) {
            System.err.println(">>> [AdbDevices] 기기 정보를 가져오는데 실패했습니다: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * 기기 제조사를 가져옵니다. (예: samsung, google)
     */
    public static String getDeviceManufacturer() {
        return getDeviceProperty("ro.product.manufacturer");
    }

    /**
     * 기기 모델명을 가져옵니다. (예: SM-F700N, sdk_gphone64_arm64)
     */
    public static String getDeviceModel() {
        return getDeviceProperty("ro.product.model");
    }

    /**
     * 기기의 Android OS 버전을 가져옵니다. (예: 12, 13, 14)
     */
    public static String getDeviceOsVersion() {
        return getDeviceProperty("ro.build.version.release");
    }
}
