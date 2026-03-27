import { describe, it, expect, beforeEach } from 'vitest';
import { saveNightsDefault, loadNightsDefault } from './nightsDefaultStore';

describe('nightsDefaultStore', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('loadNightsDefault', () => {
    it('returns 1 when nothing is stored', () => {
      expect(loadNightsDefault()).toBe(1);
    });

    it('returns 1 when stored value is not a number', () => {
      localStorage.setItem('fairnsquare_lastParticipantNights', 'not-a-number');
      expect(loadNightsDefault()).toBe(1);
    });
  });

  describe('saveNightsDefault / loadNightsDefault', () => {
    it('saves and reloads an integer nights value', () => {
      saveNightsDefault(7);
      expect(loadNightsDefault()).toBe(7);
    });

    it('overwrites a previously saved value', () => {
      saveNightsDefault(5);
      saveNightsDefault(10);
      expect(loadNightsDefault()).toBe(10);
    });

    it('rounds a fractional value stored from an old session to the nearest integer', () => {
      localStorage.setItem('fairnsquare_lastParticipantNights', '3.5');
      expect(loadNightsDefault()).toBe(4);
    });

    it('clamps a sub-1 value stored from an old session to 1', () => {
      localStorage.setItem('fairnsquare_lastParticipantNights', '0.5');
      expect(loadNightsDefault()).toBe(1);
    });
  });
});