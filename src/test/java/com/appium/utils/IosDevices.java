package com.appium.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * libimobiledevice의 ideviceinfo 명령어를 사용하여 연결된 iOS 기기의 정보를 가져오는 유틸리티 클래스입니다.
 * 사전 조건: brew install libimobiledevice ideviceinstaller
 */
@Slf4j
public class IosDevices {

    /**
     * ideviceinfo 명령어로 특정 키의 값을 가져옵니다.
     * @param key 기기 속성 키 (예: DeviceName, ProductVersion, ProductType)
     * @return 속성 값, 실패 시 "unknown"
     */
    private static String getDeviceInfo(String udid, String key) {
        try {
            String[] command = {"ideviceinfo", "-u", udid, "-k", key};
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String value = reader.readLine();
            reader.close();
            return (value != null && !value.isEmpty()) ? value.trim() : "unknown";
        } catch (Exception e) {
            log.error(">>> [IosDevices] 기기 정보[{}]를 가져오는 데 실패했습니다: {}", key, e.getMessage());
            return "unknown";
        }
    }

    /** 기기 이름을 가져옵니다. (예: xxx의 iPhone) */
    public static String getDeviceName(String udid) {
        return getDeviceInfo(udid, "DeviceName");
    }

    /** iOS 버전을 가져옵니다. (예: 17.4.1) */
    public static String getOsVersion(String udid) {
        return getDeviceInfo(udid, "ProductVersion");
    }

    /** 기기 모델 식별자를 가져옵니다. (예: iPhone15.3) */
    public static String getDeviceModel(String udid) {
        return getDeviceInfo(udid, "ProductType");
    }

    /**
     * ideviceinstaller로 설치된 앱의 버전을 가져옵니다.
     * @param bundleId 앱 번들 ID (예: com.domain.apppackage)
     * @return 앱 버전 (예: 1.27.1040600), 실패 시 "unknown"
     */
    public static String getAppVersion(String udid, String bundleId) {
        try {
            // ideviceinstaller 1.2.0 이상: list --xml -b <bundleId> 형식 사용
            String[] command = {"ideviceinstaller", "-u", udid, "list", "--xml", "-b", bundleId};
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("CFBundleShortVersionString")) {
                    String versionLine = reader.readLine();
                    if (versionLine != null) {
                        reader.close();
                        return versionLine.trim()
                                .replace("<string>", "")
                                .replace("</string>", "");
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            log.error(">>> [IosDevices] 앱 버전을 가져오는 데 실패했습니다: {}", e.getMessage());
        }
        return "unknown";
    }
}