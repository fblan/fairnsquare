package org.asymetrik.web.fairnsquare.infrastructure.captcha.api;

import java.util.Base64;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.asymetrik.web.fairnsquare.sharedkernel.validation.Violations;

import org.asymetrik.web.fairnsquare.infrastructure.captcha.api.dto.CaptchaChallengeResponseDTO;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.api.dto.CaptchaTokenResponseDTO;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.api.dto.CaptchaVerifyRequestDTO;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.domain.CaptchaChallenge;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.domain.CaptchaVerificationFailedError;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.service.CaptchaService;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.CaptchaImageGenerator;

/**
 * REST resource for the stateless CAPTCHA flow: create challenge (returns encrypted token + inline image), verify
 * answer by submitting the token and click coordinates.
 */
@ApplicationScoped
@Path("/captcha/challenges")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CaptchaResource {

    private final CaptchaService captchaService;
    private final CaptchaImageGenerator imageGenerator;

    @Inject
    public CaptchaResource(CaptchaService captchaService, CaptchaImageGenerator imageGenerator) {
        this.captchaService = captchaService;
        this.imageGenerator = imageGenerator;
    }

    /**
     * Create a new CAPTCHA challenge. Returns an AES-GCM encrypted token encoding the correct answer area bounds
     * (opaque to the client) and the challenge image inline as base64-encoded PNG.
     *
     * @return 201 Created with {@code { challengeToken, imageBase64 }}
     */
    @POST
    public Response createChallenge() {
        CaptchaChallenge challenge = captchaService.generateChallenge();
        byte[] png = imageGenerator.generate(challenge);
        String imageBase64 = Base64.getEncoder().encodeToString(png);
        String challengeToken = captchaService.encryptChallenge(challenge);
        String secretFingerprint = captchaService.getSecretFingerprint();
        return Response.status(Response.Status.CREATED)
                .entity(new CaptchaChallengeResponseDTO(challengeToken, imageBase64, secretFingerprint)).build();
    }

    /**
     * Verify a CAPTCHA answer. The client submits the encrypted challenge token alongside the click coordinates. The
     * server decrypts the token, checks expiry, and verifies the coordinates against the stored correct area.
     *
     * @param request
     *            contains {@code challengeToken}, {@code x}, and {@code y}
     *
     * @return 200 OK with signed proof token, or 400 if wrong answer or expired
     */
    @POST
    @Path("/verify")
    public Response verifyChallenge(CaptchaVerifyRequestDTO request) {
        Violations.valid(request);
        String token = captchaService.verifyAnswer(request.getChallengeToken(), request.getX(), request.getY())
                .orElseThrow(CaptchaVerificationFailedError::new);
        return Response.ok(new CaptchaTokenResponseDTO(token)).build();
    }
}
