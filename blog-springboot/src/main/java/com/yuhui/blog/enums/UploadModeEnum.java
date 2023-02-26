package com.yuhui.blog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 上传模式枚举
 */
@Getter
@AllArgsConstructor
public enum UploadModeEnum {
    /**
     * oss
     */
    OSS("oss", "ossUploadStrategyImpl"),
    /**
     * 本地
     */
    LOCAL("local", "localUploadStrategyImpl"),
    /**
     * cos
     */
    COS("cos", "cosUploadStrategyImpl");

    /**
     * 模式
     */
    private final String mode;

    /**
     * 策略
     */
    private final String strategy;

    /**
     * 根据模式获取策略
     */
    public static String getStrategy(String mode) {
        for (UploadModeEnum value : UploadModeEnum.values()) {
            if (value.getMode().equals(mode)) {
                return value.getStrategy();
            }
        }
        return null;
    }

}
