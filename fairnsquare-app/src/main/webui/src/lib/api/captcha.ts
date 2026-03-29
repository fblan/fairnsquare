/**
 * CAPTCHA API client
 */

import { apiRequest } from './client';

export interface CaptchaChallengeResponse {
  challengeToken: string;
  imageBase64: string;
  secretFingerprint: string;
}

export interface CaptchaTokenResponse {
  token: string;
}

/**
 * Create a new CAPTCHA challenge on the server.
 * Returns the encrypted challenge token and the CAPTCHA image as a base64-encoded PNG.
 */
export async function createChallenge(): Promise<CaptchaChallengeResponse> {
  return apiRequest<CaptchaChallengeResponse>('/captcha/challenges', {
    method: 'POST',
  });
}

/**
 * Verify a CAPTCHA answer by submitting the encrypted challenge token and click coordinates.
 * Returns the signed proof token on success, throws ApiError on wrong answer.
 */
export async function verifyChallenge(
  challengeToken: string,
  x: number,
  y: number
): Promise<CaptchaTokenResponse> {
  return apiRequest<CaptchaTokenResponse>('/captcha/challenges/verify', {
    method: 'POST',
    body: JSON.stringify({ challengeToken, x, y }),
  });
}
