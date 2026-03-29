import { describe, it, expect, beforeEach } from 'vitest';
import { saveToken, loadToken, clearToken, hasValidToken } from './captchaStore';

// Build a token with a given exp (epoch seconds) using the same format as the backend:
// base64url({"exp":<n>}).<fake-sig>
function makeToken(expOffsetSeconds: number): string {
  const exp = Math.floor(Date.now() / 1000) + expOffsetSeconds;
  const payload = btoa(JSON.stringify({ exp }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `${payload}.fakesig`;
}

describe('captchaStore', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  // --- loadToken ---

  describe('loadToken', () => {
    it('returns null when nothing stored', () => {
      expect(loadToken()).toBeNull();
    });

    it('returns the token when it is valid and not expired', () => {
      const token = makeToken(3600);
      saveToken(token);
      expect(loadToken()).toBe(token);
    });

    it('returns null and removes token when expired', () => {
      const token = makeToken(-1);
      saveToken(token);
      expect(loadToken()).toBeNull();
      expect(localStorage.getItem('fairnsquare_captchaToken')).toBeNull();
    });

    it('returns null when token is malformed', () => {
      localStorage.setItem('fairnsquare_captchaToken', 'not-a-valid-token');
      expect(loadToken()).toBeNull();
    });
  });

  // --- saveToken / clearToken ---

  describe('saveToken', () => {
    it('persists the token in localStorage', () => {
      const token = makeToken(3600);
      saveToken(token);
      expect(localStorage.getItem('fairnsquare_captchaToken')).toBe(token);
    });

    it('overwrites a previously saved token', () => {
      saveToken(makeToken(3600));
      const newer = makeToken(7200);
      saveToken(newer);
      expect(localStorage.getItem('fairnsquare_captchaToken')).toBe(newer);
    });
  });

  describe('clearToken', () => {
    it('removes the token from localStorage', () => {
      saveToken(makeToken(3600));
      clearToken();
      expect(localStorage.getItem('fairnsquare_captchaToken')).toBeNull();
    });

    it('does nothing when nothing is stored', () => {
      expect(() => clearToken()).not.toThrow();
    });
  });

  // --- hasValidToken ---

  describe('hasValidToken', () => {
    it('returns false when no token stored', () => {
      expect(hasValidToken()).toBe(false);
    });

    it('returns true when a valid token is stored', () => {
      saveToken(makeToken(3600));
      expect(hasValidToken()).toBe(true);
    });

    it('returns false when stored token is expired', () => {
      saveToken(makeToken(-1));
      expect(hasValidToken()).toBe(false);
    });
  });
});
