/**
 * The signed token the server gives when the CAPTCHA is solved. Creating a split needs it.
 * Token format: base64url({"exp": <epoch seconds>}).<hmac>. Same localStorage key as the former
 * lib/stores/captchaStore.ts.
 */

const CAPTCHA_TOKEN_KEY = 'fairnsquare_captchaToken';

/** The header that carries the token. */
export const CAPTCHA_TOKEN_HEADER = 'X-Captcha-Token';

/** @param {string} token */
export function saveToken(token) {
  localStorage.setItem(CAPTCHA_TOKEN_KEY, token);
}

/** @returns {string | null} the stored token if it has not expired */
export function loadToken() {
  const token = localStorage.getItem(CAPTCHA_TOKEN_KEY);
  if (!token) return null;
  if (!isExpired(token)) return token;
  localStorage.removeItem(CAPTCHA_TOKEN_KEY);
  return null;
}

export function clearToken() {
  localStorage.removeItem(CAPTCHA_TOKEN_KEY);
}

/** Expired, or unreadable. */
function isExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[0].replace(/-/g, '+').replace(/_/g, '/')));
    return Math.floor(Date.now() / 1000) > payload.exp;
  } catch {
    return true;
  }
}
