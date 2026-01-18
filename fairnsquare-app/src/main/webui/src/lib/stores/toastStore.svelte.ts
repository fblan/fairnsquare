/**
 * Toast notification store using Svelte 5 runes
 * Provides global toast/notification state management
 */

export interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number;
}

// Global toast state using Svelte 5 runes
let toasts = $state<Toast[]>([]);

export function getToasts(): Toast[] {
  return toasts;
}

export function addToast(toast: Omit<Toast, 'id'>): void {
  const id = crypto.randomUUID();
  const newToast: Toast = { ...toast, id };

  toasts = [...toasts, newToast];

  // Auto-remove after duration (default 5 seconds)
  const duration = toast.duration ?? 5000;
  setTimeout(() => {
    removeToast(id);
  }, duration);
}

export function removeToast(id: string): void {
  toasts = toasts.filter((t) => t.id !== id);
}

export function clearToasts(): void {
  toasts = [];
}