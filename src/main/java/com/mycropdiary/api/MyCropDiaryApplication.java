package com.mycropdiary.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

@SpringBootApplication
public class MyCropDiaryApplication {
    public static void main(String[] args) {
        loadDotenvFiles();
        SpringApplication.run(MyCropDiaryApplication.class, args);
    }

    private static void loadDotenvFiles() {
        for (String filename : new String[]{".env", ".env.local"}) {
            File envFile = new File(filename);
            if (envFile.exists() && envFile.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                            int idx = line.indexOf('=');
                            String key = line.substring(0, idx).trim();
                            String value = line.substring(idx + 1).trim();
                            if (!key.isEmpty() && !value.isEmpty()) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}
