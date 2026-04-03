import { describe, it, expect, beforeEach } from 'vitest';
import { saveLastSplit, loadLastSplits, removeLastSplit } from './lastSplitStore';

describe('lastSplitStore', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('loadLastSplits', () => {
    it('returns empty array when nothing is stored', () => {
      expect(loadLastSplits()).toEqual([]);
    });

    it('returns empty array when stored value is corrupt JSON', () => {
      localStorage.setItem('fairnsquare_lastSplits', 'not-json');
      expect(loadLastSplits()).toEqual([]);
    });

    it('returns empty array when stored value is not an array', () => {
      localStorage.setItem('fairnsquare_lastSplits', JSON.stringify({ id: 'x', name: 'y' }));
      expect(loadLastSplits()).toEqual([]);
    });
  });

  describe('saveLastSplit', () => {
    it('saves a split and returns it as the first entry', () => {
      saveLastSplit({ id: 'abc', name: 'Weekend Trip' });
      expect(loadLastSplits()).toEqual([{ id: 'abc', name: 'Weekend Trip' }]);
    });

    it('prepends new split to the front of the list', () => {
      saveLastSplit({ id: 'first', name: 'First' });
      saveLastSplit({ id: 'second', name: 'Second' });
      expect(loadLastSplits()[0]).toEqual({ id: 'second', name: 'Second' });
      expect(loadLastSplits()[1]).toEqual({ id: 'first', name: 'First' });
    });

    it('deduplicates by id — moves existing split to front', () => {
      saveLastSplit({ id: 'a', name: 'A' });
      saveLastSplit({ id: 'b', name: 'B' });
      saveLastSplit({ id: 'a', name: 'A updated' });
      const splits = loadLastSplits();
      expect(splits).toHaveLength(2);
      expect(splits[0]).toEqual({ id: 'a', name: 'A updated' });
      expect(splits[1]).toEqual({ id: 'b', name: 'B' });
    });

    it('caps the list at 10 entries', () => {
      for (let i = 0; i < 12; i++) {
        saveLastSplit({ id: `split-${i}`, name: `Split ${i}` });
      }
      expect(loadLastSplits()).toHaveLength(10);
    });

    it('keeps the most recent 10 when cap is exceeded', () => {
      for (let i = 0; i < 12; i++) {
        saveLastSplit({ id: `split-${i}`, name: `Split ${i}` });
      }
      const splits = loadLastSplits();
      expect(splits[0].id).toBe('split-11');
      expect(splits[9].id).toBe('split-2');
    });
  });

  describe('removeLastSplit', () => {
    it('removes the split with the given id', () => {
      saveLastSplit({ id: 'a', name: 'A' });
      saveLastSplit({ id: 'b', name: 'B' });
      removeLastSplit('a');
      expect(loadLastSplits()).toEqual([{ id: 'b', name: 'B' }]);
    });

    it('does nothing when the id is not found', () => {
      saveLastSplit({ id: 'a', name: 'A' });
      removeLastSplit('unknown');
      expect(loadLastSplits()).toHaveLength(1);
    });

    it('does nothing when storage is empty', () => {
      expect(() => removeLastSplit('x')).not.toThrow();
      expect(loadLastSplits()).toEqual([]);
    });
  });
});
