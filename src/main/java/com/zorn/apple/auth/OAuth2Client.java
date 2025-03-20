package com.zorn.apple.auth;

import com.zorn.json.JsonUtility;
import com.zorn.jwt.JwtUtility;
import io.jsonwebtoken.Jwts;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

public class OAuth2Client {
    private String clientSecret;
    private static JSONArray publicKeys;
    private final OAuth2ClientCredentials credentials;
    private static final String APPLE_ISSUER_URL = "https://appleid.apple.com";
    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String APPLE_PUBLIC_KEY_URL = "https://appleid.apple.com/auth/oauth2/v2/keys";

    public OAuth2Client(OAuth2ClientCredentials credentials) {
        this.credentials = credentials;
    }

    private void decodeIdToken(String idToken) {

    }

    private static JSONArray updatePublicKeys() {
        try(HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(new URI(OAuth2Client.APPLE_PUBLIC_KEY_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                // TODO: must throw error
                return null;
            }

            JSONObject responseBody = new JSONObject(response.body());

            if (!responseBody.has("keys")) {
                return null;
            }

            JSONArray publicKeys = responseBody.getJSONArray("keys");

            OAuth2Client.publicKeys = publicKeys;
            return publicKeys;
        } catch (InterruptedException | URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AppleToken retrieveTokenFromAuthCode(String authorizationCode) {
        this.updateClientSecret();

        String requestBodyContent = String.format(
                "client_id=%s&client_secret=%s&code=%s&grant_type=%s",
                this.credentials.clientId(),
                this.clientSecret,
                authorizationCode,
                "authorization_code"
        );
        HttpRequest.BodyPublisher requestBody = HttpRequest.BodyPublishers.ofString(requestBodyContent);

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(new URI(OAuth2Client.APPLE_TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(requestBody)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                // TODO: must throw error
                return null;
            }

            JSONObject responseBody = new JSONObject(response.body());

            return JsonUtility.parseJsonToRecord(responseBody, AppleToken.class);
        } catch (InterruptedException | URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateClientSecret() {
        if (this.clientSecret == null || this.clientSecret.isEmpty()) {
            this.clientSecret = this.generateClientSecret();
            return;
        }

        long currentTimeInSeconds = Instant.now().getEpochSecond();
        long validationTime = currentTimeInSeconds + 120;

        try {
            JSONObject jwtPayload = JwtUtility.decodeJwt(this.clientSecret);
            if (!jwtPayload.has("exp")) throw new RuntimeException();

            long exp = jwtPayload.getLong("exp");
            if (validationTime >= exp) {
                this.clientSecret = this.generateClientSecret();
            }
        } catch (IllegalArgumentException e) {
            this.clientSecret = this.generateClientSecret();
        }
    }

    private String generateClientSecret() {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiration = Date.from(now.plusSeconds(60 * 60 * 24));

        PrivateKey privateKey = this.getPrivateKey();

        return Jwts.builder()
                .header()
                .keyId(credentials.keyId())
                .and()
                .issuer(this.credentials.teamId())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .audience().add(APPLE_ISSUER_URL).and()
                .subject(this.credentials.clientId())
                .signWith(privateKey)
                .compact();
    }

    private PrivateKey getPrivateKey() {
        try {
            String cleanPrivateKey = this.credentials.privateKey()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] privateKeyBytes = Base64.getDecoder().decode(cleanPrivateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");

            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
