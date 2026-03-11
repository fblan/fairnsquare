import { describe, it, expect, beforeEach } from 'vitest';
import { saveLastSplit, loadLastSplit, clearLastSplit } from './lastSplitStore';

describe('lastSplitStore', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('loadLastSplit', () => {
    it('returns null when nothing is stored', () => {
      expect(loadLastSplit()).toBeNull();
    });

    it('returns null when stored value is corrupt JSON', () => {
      localStorage.setItem('fairnsquare_lastSplit', 'not-json');
      expect(loadLastSplit()).toBeNull();
    });
  });

  describe('saveLastSplit / loadLastSplit', () => {
    it('saves and reloads a split', () => {
      saveLastSplit({ id: 'abc123', name: 'Weekend Trip' });
      expect(loadLastSplit()).toEqual({ id: 'abc123', name: 'Weekend Trip' });
    });

    it('overwrites a previously saved split', () => {
      saveLastSplit({ id: 'old', name: 'Old Split' });
      saveLastSplit({ id: 'new', name: 'New Split' });
      expect(loadLastSplit()).toEqual({ id: 'new', name: 'New Split' });
    });
  });

  describe('clearLastSplit', () => {
    it('removes the stored split', () => {
      saveLastSplit({ id: 'abc123', name: 'Weekend Trip' });
      clearLastSplit();
      expect(loadLastSplit()).toBeNull();
    });

    it('does nothing when nothing is stored', () => {
      expect(() => clearLastSplit()).not.toThrow();
      expect(loadLastSplit()).toBeNull();
    });
  });
});
