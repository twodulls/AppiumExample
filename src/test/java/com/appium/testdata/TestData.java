package com.appium.testdata;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Data
public class TestData {
    private String ipAddress;
    private int port;
    private String id;
    private String pw;
    private AndroidData android;
    private IosData ios;

    @Data
    public static class AndroidData {
        private String appPackage;
    }

    @Data
    public static class IosData {
        private String bundleId;
        private String udid;
        private String xcodeOrgId;
    }

    @Override
    public String toString(){
        return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE);
    }
}