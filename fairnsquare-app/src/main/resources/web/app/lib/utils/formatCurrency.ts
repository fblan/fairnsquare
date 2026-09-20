/**
 * Currency formatting utilities
 * Default currency: EUR (as per PRD)
 */

const DEFAULT_CURRENCY = 'EUR';
const DEFAULT_LOCALE = 'fr-FR';

export function formatCurrency(
  amount: number,
  currency: string = DEFAULT_CURRENCY,
  locale: string = DEFAULT_LOCALE
): string {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

export function parseCurrency(value: string): number {
  // Remove currency symbols and spaces, replace comma with dot
  const cleaned = value.replace(/[^0-9,.-]/g, '').replace(',', '.');
  return parseFloat(cleaned) || 0;
}