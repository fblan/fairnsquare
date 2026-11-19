/**
 * The nights of the last participant added, offered for the next one (people often stay the same time).
 * Same localStorage key as the former lib/stores/nightsDefaultStore.ts.
 */

const NIGHTS_DEFAULT_KEY = 'fairnsquare_lastParticipantNights';

/** @param {number} nights */
export function saveNightsDefault(nights) {
  localStorage.setItem(NIGHTS_DEFAULT_KEY, String(nights));
}

/** @returns {number} a whole number of nights, at least 1 */
export function loadNightsDefault() {
  const stored = parseFloat(localStorage.getItem(NIGHTS_DEFAULT_KEY) ?? '');
  return Number.isNaN(stored) ? 1 : Math.max(1, Math.round(stored));
}
