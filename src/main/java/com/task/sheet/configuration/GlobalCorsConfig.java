package com.task.sheet.configuration;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

@Configuration
public class GlobalCorsConfig {

    @Value("${google.credentials.json-path}")
    private String jsonPath;

    @Value("${google.application-name:gsheet-export}")
    private String appName;

    @Bean
    public ServiceAccountCredentials googleServiceAccountCredentials() throws Exception {
        return (ServiceAccountCredentials) ServiceAccountCredentials
                .fromStream(new FileInputStream(jsonPath))
                .createScoped(List.of(
                        DriveScopes.DRIVE_READONLY,
                        SheetsScopes.SPREADSHEETS_READONLY
                ));
    }

    @Bean
    public Drive drive(ServiceAccountCredentials creds) {
        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(creds))
                .setApplicationName(appName)
                .build();
    }

    @Bean
    public Sheets sheets(ServiceAccountCredentials creds) {
        return new Sheets.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(creds))
                .setApplicationName(appName)
                .build();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",
                "https://datteamwork.com",
                "https://datteamwork.com:443",
                "http://localhost:5001",
                "http://localhost:5000",
                "http://104.251.223.167:5000",
                "https://deetaanalyticsllc.com:8053",
                "http://104.237.9.43:8053",
                "https://104.237.9.43:8053",
                "https://deetaanalyticsllc.com:8054",
                "http://104.237.9.43:8054",
                "https://104.237.9.43:8054"
        ));

        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
