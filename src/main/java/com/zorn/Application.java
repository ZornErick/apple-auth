package com.zorn;

import com.zorn.apple.auth.OAuth2Client;
import com.zorn.apple.auth.OAuth2ClientCredentials;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Application {
    public static void main(String[] args) throws IOException, IllegalAccessException {
        Path privateKeyPath = Paths.get("AuthKey_BP2N43Y5ZB.p8");
        String privateKey = Files.readString(privateKeyPath);

        OAuth2Client oAuth2Client = new OAuth2Client(
                new OAuth2ClientCredentials(
                        "4QM33NU23W",
                        "com.nectar.toquetoque.app",
                        privateKey,
                        "BP2N43Y5ZB"
                )
        );
    }
}
