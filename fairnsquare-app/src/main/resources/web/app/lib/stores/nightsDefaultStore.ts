const NIGHTS_DEFAULT_KEY = 'fairnsquare_lastParticipantNights';

export function saveNightsDefault(nights: number): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem(NIGHTS_DEFAULT_KEY, nights.toString());
  }
}

export function loadNightsDefault(): number {
  if (typeof window === 'undefined') return 1;
  const stored = localStorage.getItem(NIGHTS_DEFAULT_KEY);
  if (!stored) return 1;
  const parsed = parseFloat(stored);
  if (isNaN(parsed)) return 1;
  return Math.max(1, Math.round(parsed));
}