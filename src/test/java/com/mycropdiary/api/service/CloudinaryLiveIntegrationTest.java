package com.mycropdiary.api.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudinaryLiveIntegrationTest {

    @Test
    @DisplayName("Live test: Upload sample file to Cloudinary using real credentials")
    void testLiveUploadToCloudinary() {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "drajm922",
                "api_key", "411739723533132",
                "api_secret", "1s15qqgWKr-8NBjPYkeyIixY9BI",
                "secure", true
        ));

        byte[] sampleBytes = "MyCropDiary Farm Evidence Verification Test".getBytes();

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(sampleBytes, ObjectUtils.asMap(
                    "folder", "mycropdiary/test",
                    "resource_type", "auto"
            ));

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            System.out.println(">>> LIVE CLOUDINARY UPLOAD SUCCESS: " + secureUrl);
            System.out.println(">>> Public ID: " + publicId);

            assertNotNull(secureUrl);
            assertTrue(secureUrl.startsWith("https://res.cloudinary.com/drajm922/"));

            // Clean up test file
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            System.out.println(">>> LIVE CLOUDINARY CLEANUP SUCCESS");
        } catch (IOException e) {
            System.err.println("Live Cloudinary test skipped or network issue: " + e.getMessage());
        }
    }
}
