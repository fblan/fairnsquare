/**
 * CAPTCHA token store — persists the signed token in localStorage and checks its expiry.
 * Token format: base64url({"exp":<epoch_seconds>}).<hmac>
 */

const CAPTCHA_TOKEN_KEY = 'fairnsquare_captchaToken';

export const CAPTCHA_TOKEN_HEADER = 'X-Captcha-Token';

/**
 * Persist the token in localStorage.
 */
export function saveToken(token: string): void {
  localStorage.setItem(CAPTCHA_TOKEN_KEY, token);
}

/**
 * Return the stored token if it exists and has not expired, null otherwise.
 */
export function loadToken(): string | null {
  const token = localStorage.getItem(CAPTCHA_TOKEN_KEY);
  if (!token) return null;
  if (!isTokenExpired(token)) return token;
  localStorage.removeItem(CAPTCHA_TOKEN_KEY);
  return null;
}

/**
 * Remove the token from localStorage.
 */
export function clearToken(): void {
  localStorage.removeItem(CAPTCHA_TOKEN_KEY);
}

/**
 * Returns true if a valid, non-expired token is present in localStorage.
 */
export function hasValidToken(): boolean {
  return loadToken() !== null;
}

/**
 * Parse the expiry from the token payload and return true if the token has expired.
 * Returns true (treat as expired) on any parse error.
 */
function isTokenExpired(token: string): boolean {
  try {
    const payloadB64 = token.split('.')[0];
    const json = atob(payloadB64.replace(/-/g, '+').replace(/_/g, '/'));
    const payload = JSON.parse(json) as { exp: number };
    return Math.floor(Date.now() / 1000) > payload.exp;
  } catch {
    return true;
  }
}
