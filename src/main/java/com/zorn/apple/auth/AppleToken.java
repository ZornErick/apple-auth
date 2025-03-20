package com.zorn.apple.auth;

public record AppleToken(String accessToken, Long expiresIn, String idToken, String refreshToken, String tokenType) {
}
