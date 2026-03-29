package org.asymetrik.web.fairnsquare.infrastructure.captcha.api;

import java.util.Base64;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.asymetrik.web.fairnsquare.infrastructure.captcha.api.dto.CaptchaChallengeResponseDTO;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.api.dto.CaptchaTokenResponseDTO;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.api.dto.CaptchaVerifyRequestDTO;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.domain.CaptchaChallenge;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.domain.CaptchaVerificationFailedError;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.service.CaptchaService;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.CaptchaImageGenerator;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * REST resource for the stateless CAPTCHA flow: create challenge (returns encrypted token + inline image), verify
 * answer by submitting the token and click coordinates.
 */
@Path("/api/captcha/challenges")
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
    @Operation(summary = "Create a CAPTCHA challenge")
    @APIResponse(responseCode = "201", description = "Challenge created with encrypted token and inline image")
    public Response createChallenge() {
        CaptchaChallenge challenge = captchaService.generateChallenge();
        byte[] png = imageGenerator.generate(challenge);
        String imageBase64 = Base64.getEncoder().encodeToString(png);
        String challengeToken = captchaService.encryptChallenge(challenge);
        return Response.status(Response.Status.CREATED)
                .entity(new CaptchaChallengeResponseDTO(challengeToken, imageBase64)).build();
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
    @Operation(summary = "Verify a CAPTCHA answer by coordinates")
    @APIResponse(responseCode = "200", description = "Correct answer — signed token returned")
    @APIResponse(responseCode = "400", description = "Wrong answer or expired challenge token")
    public Response verifyChallenge(@Valid CaptchaVerifyRequestDTO request) {
        String token = captchaService.verifyAnswer(request.getChallengeToken(), request.getX(), request.getY())
                .orElseThrow(CaptchaVerificationFailedError::new);
        return Response.ok(new CaptchaTokenResponseDTO(token)).build();
    }
}
