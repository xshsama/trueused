package com.xsh.trueused.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map<String, Object> getUploadSignature() {
        long timestamp = System.currentTimeMillis() / 1000L;
        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);

        // 你可以在这里添加更多的参数来自定义上传行为，例如：
        // paramsToSign.put("upload_preset", "your_upload_preset");
        // paramsToSign.put("folder", "products");

        String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);

        Map<String, Object> signatureData = new HashMap<>();
        signatureData.put("timestamp", timestamp);
        signatureData.put("signature", signature);
        signatureData.put("api_key", cloudinary.config.apiKey);

        return signatureData;
    }
}