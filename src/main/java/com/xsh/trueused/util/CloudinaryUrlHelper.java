package com.xsh.trueused.util;

public class CloudinaryUrlHelper {

    private static final String BASE_URL = "https://res.cloudinary.com/dhgbkfhwo/image/upload/";

    public static String getUrl(String imageKey) {
        return BASE_URL + imageKey;
    }

    public static String optimize(String imageKey) {
        return BASE_URL + "f_auto,q_auto/" + imageKey;
    }

    public static String getThumbnailUrl(String imageKey) {
        return BASE_URL + "f_auto,q_auto,w_400,c_limit/" + imageKey;
    }

    public static String getDetailUrl(String imageKey) {
        return BASE_URL + "f_auto,q_auto,w_800,c_limit/" + imageKey;
    }

    public static String getBlurUrl(String imageKey) {
        return BASE_URL + "f_auto,q_auto,w_50,e_blur:1000/" + imageKey;
    }
}
