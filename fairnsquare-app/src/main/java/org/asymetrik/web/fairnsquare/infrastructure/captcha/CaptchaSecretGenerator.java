package org.asymetrik.web.fairnsquare.infrastructure.captcha;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Standalone utility to generate a cryptographically secure value for the {@code CAPTCHA_SECRET} environment variable.
 * Run once, copy the output into your {@code .env} file or deployment secrets.
 *
 * <pre>
 * mvn compile exec:java -Dexec.mainClass=org.asymetrik.web.fairnsquare.infrastructure.captcha.CaptchaSecretGenerator
 * </pre>
 */
public class CaptchaSecretGenerator {

    public static void main(String[] args) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String secret = HexFormat.of().formatHex(bytes);
        System.out.println("CAPTCHA_SECRET=" + secret);
    }
}
