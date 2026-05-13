package com.xsh.trueused.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudinaryUrlHelper {

    private static String cloudName = "demo";

    @Value("${cloudinary.cloud_name:demo}")
    public void setCloudName(String configuredCloudName) {
        cloudName = configuredCloudName;
    }

    public static String getUrl(String imageKey) {
        return baseUrl() + imageKey;
    }

    public static String optimize(String imageKey) {
        return baseUrl() + "f_auto,q_auto/" + imageKey;
    }

    public static String getThumbnailUrl(String imageKey) {
        return baseUrl() + "f_auto,q_auto,w_400,c_limit/" + imageKey;
    }

    public static String getDetailUrl(String imageKey) {
        return baseUrl() + "f_auto,q_auto,w_800,c_limit/" + imageKey;
    }

    public static String getBlurUrl(String imageKey) {
        return baseUrl() + "f_auto,q_auto,w_50,e_blur:1000/" + imageKey;
    }

    private static String baseUrl() {
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/";
    }
}
