import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Toaster from './Toaster.svelte';
import { addToast, clearToasts } from '$lib/stores/toastStore.svelte';

describe('Toaster', () => {
  beforeEach(() => {
    clearToasts();
  });

  it('renders nothing when there are no toasts', () => {
    render(Toaster);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('renders a toast message when addToast is called', async () => {
    render(Toaster);
    addToast({ type: 'success', message: 'Saved successfully!' });

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText('Saved successfully!')).toBeInTheDocument();
    });
  });

  it('renders multiple toasts', async () => {
    render(Toaster);
    addToast({ type: 'success', message: 'First toast' });
    addToast({ type: 'error', message: 'Second toast' });

    await waitFor(() => {
      expect(screen.getAllByRole('alert')).toHaveLength(2);
      expect(screen.getByText('First toast')).toBeInTheDocument();
      expect(screen.getByText('Second toast')).toBeInTheDocument();
    });
  });

  it('shows a dismiss button on each toast', async () => {
    render(Toaster);
    addToast({ type: 'info', message: 'Hello' });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Dismiss' })).toBeInTheDocument();
    });
  });

  it('removes toast when dismiss button is clicked', async () => {
    render(Toaster);
    addToast({ type: 'info', message: 'Dismiss me' });

    await waitFor(() => {
      expect(screen.getByText('Dismiss me')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }));

    await waitFor(() => {
      expect(screen.queryByText('Dismiss me')).not.toBeInTheDocument();
    });
  });

  it('applies success styles for type success', async () => {
    render(Toaster);
    addToast({ type: 'success', message: 'Done!' });

    await waitFor(() => {
      expect(screen.getByRole('alert').className).toContain('bg-green-600');
    });
  });

  it('applies error styles for type error', async () => {
    render(Toaster);
    addToast({ type: 'error', message: 'Oops!' });

    await waitFor(() => {
      expect(screen.getByRole('alert').className).toContain('bg-destructive');
    });
  });

  it('applies warning styles for type warning', async () => {
    render(Toaster);
    addToast({ type: 'warning', message: 'Watch out!' });

    await waitFor(() => {
      expect(screen.getByRole('alert').className).toContain('bg-yellow-500');
    });
  });

  it('applies info styles for type info', async () => {
    render(Toaster);
    addToast({ type: 'info', message: 'FYI' });

    await waitFor(() => {
      expect(screen.getByRole('alert').className).toContain('bg-primary');
    });
  });

  it('has aria-live polite on the container', () => {
    const { container } = render(Toaster);

    expect(container.querySelector('[aria-live="polite"]')).toBeInTheDocument();
  });

  it('renders description line when description is provided', async () => {
    render(Toaster);
    addToast({ type: 'success', message: 'Expense added', description: 'Lunch · €12.50 · Paid by Alice' });

    await waitFor(() => {
      expect(screen.getByText('Expense added')).toBeInTheDocument();
      expect(screen.getByText('Lunch · €12.50 · Paid by Alice')).toBeInTheDocument();
    });
  });

  it('does not render description line when description is absent', async () => {
    render(Toaster);
    addToast({ type: 'success', message: 'Done!' });

    await waitFor(() => {
      expect(screen.getByText('Done!')).toBeInTheDocument();
    });

    // Only one text node inside the alert — no description paragraph
    const alert = screen.getByRole('alert');
    expect(alert.querySelectorAll('p')).toHaveLength(1);
  });
});