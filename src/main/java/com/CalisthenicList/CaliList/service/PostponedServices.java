package com.CalisthenicList.CaliList.service;

import lombok.SneakyThrows;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class PostponedServices {
    @SneakyThrows
    //INFO Postponed - feature potentially too bothersome for users (sets secure passwords as pawned when they aren't)
    //INFO PWNED PASSWORDS require annotation about the source
    private HttpStatusCode checkIfPwnedPassword(String password) {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(password.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            hexString.append(String.format("%02x", b));
        }
        String SHA1Password = hexString.substring(0, 5);
        String pwnedPasswordUrl = "https://api.pwnedpasswords.com/range/";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(pwnedPasswordUrl + SHA1Password, String.class);
        return response.getStatusCode();
    }
}
