package com.vote.VotingSystem.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class FlutterwaveService {
    @Value("${flutterwave.publicKey}")
    private String publicKey;

    @Value("${flutterwave.secretKey}")
    private String secretKey;

    @Value("${flutterwave.encryptionKey}")
    private String encryptionKey;

    private final String baseUrl = "https://api.flutterwave.com/v3";

    public String initiatePayment(String candidateId, Double amount, String email, String name, String phoneNumber, String redirectUrl, String paymentMethod) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + secretKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tx_ref", "vote_" + candidateId + "_" + System.currentTimeMillis());
        payload.put("amount", amount);
        //payload.put("currency", "XOF");
        payload.put("redirect_url", redirectUrl);
        payload.put("payment_options", paymentMethod);
        payload.put("meta", Map.of("consumer_id", candidateId, "consumer_mac", "92a3-912ba-1192a"));
        payload.put("customer", Map.of(
                "email", email,
                "phonenumber", phoneNumber,
                "name", name
        ));
        payload.put("customizations", Map.of(
                "title", "Vote en ligne",
                "description", "Paiement pour un vote",
                "logo", "https://votre-logo.com/logo.png"
        ));
        payload.put("amount", amount);
        payload.put("currency", "XAF");

        // Gestion des méthodes de paiement spécifiques au Cameroun
        switch (paymentMethod.toLowerCase()) {
            case "mobile money":
                payload.put("payment_options", "mobilemoneycm");
                break;
            case "orange money":
                payload.put("payment_options", "orangemoney");
                break;
            default:
                throw new IllegalArgumentException("Méthode de paiement non supportée");
        }


        String encryptedPayload = encryptPayload(payload);
        Map<String, String> encryptedBody = new HashMap<>();
        encryptedBody.put("client", encryptedPayload);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(encryptedBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/charges?type=mobile_money_franco", entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return (String) ((Map) response.getBody().get("data")).get("link");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String encryptPayload(Map<String, Object> payload) {
        try {
            String jsonPayload = new ObjectMapper().writeValueAsString(payload);
            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encryptedBytes = cipher.doFinal(jsonPayload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chiffrement du payload", e);
        }
    }

    public boolean verifyTransaction(String transactionId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + secretKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/transactions/" + transactionId + "/verify",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                return "successful".equals(data.get("status"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
