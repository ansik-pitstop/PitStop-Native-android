package com.pitstop.utils;

import com.pitstop.BuildConfig;

public class SecretUtils {

    static {
        System.loadLibrary("secret");
    }

    /**
     * Pitstop
     */

    private static native String getPitstopEndpointStaging();

    private static native String getPitstopEndpointSnapshot();

    private static native String getPitstopEndpointRelease();

    public static String getEndpointUrl() {
        switch (BuildConfig.ENDPOINT_TYPE) {
            case BuildConfig.ENDPOINT_TYPE_RELEASE:
                return getPitstopEndpointRelease();
            case BuildConfig.ENDPOINT_TYPE_STAGING:
                return getPitstopEndpointStaging();
            case BuildConfig.ENDPOINT_TYPE_SNAPSHOT:
                return getPitstopEndpointSnapshot();
            default:
                return "";
        }
    }

    private static native String getPitstopClientIdDebug();

    private static native String getPitstopClientIdRelease();

    public static String getClientId() {
        return BuildConfig.ENDPOINT_TYPE.equals(BuildConfig.ENDPOINT_TYPE_RELEASE)
                ? getPitstopClientIdRelease() : getPitstopClientIdDebug();
    }

    /**
     * Mixpanel
     */

    private static native String getMixpanelTokenDev();

    private static native String getMixpanelTokenProd();

    public static String getMixpanelToken() {
        return BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE)
                ? getMixpanelTokenProd() : getMixpanelTokenDev();
    }

    /**
     * Smooch
     */

    private static native String getSmoochTokenDev();

    private static native String getSmoochTokenProd();

    public static String getSmoochToken() {
        return BuildConfig.ENDPOINT_TYPE.equals(BuildConfig.ENDPOINT_TYPE_RELEASE)
                ? getSmoochTokenProd() : getSmoochTokenDev();
    }

    /**
     * Parse
     */

    private static native String getParseAppIdDev();

    private static native String getParseAppIdProd();

    public static String getParseAppId() {
        return BuildConfig.ENDPOINT_TYPE.equals(BuildConfig.ENDPOINT_TYPE_RELEASE)
                ? getParseAppIdProd() : getParseAppIdDev();
    }

    /**
     * Mashape
     */

    private static native String getMashapeKey();

}
