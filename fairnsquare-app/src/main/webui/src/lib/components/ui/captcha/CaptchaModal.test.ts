import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import CaptchaModal from './CaptchaModal.svelte';

vi.mock('$lib/api/captcha', () => ({
  createChallenge: vi.fn(),
  verifyChallenge: vi.fn(),
}));

vi.mock('$lib/stores/captchaStore', () => ({
  saveToken: vi.fn(),
  CAPTCHA_TOKEN_HEADER: 'X-Captcha-Token',
}));

import { createChallenge, verifyChallenge } from '$lib/api/captcha';
import { saveToken } from '$lib/stores/captchaStore';

describe('CaptchaModal', () => {
  const defaultProps = {
    open: true,
    onSuccess: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(createChallenge).mockResolvedValue({
      challengeToken: 'encrypted-challenge-token',
      imageBase64: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    });
  });

  // --- Rendering ---

  it('renders dialog when open is true', async () => {
    render(CaptchaModal, { props: defaultProps });

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
    expect(screen.getByText('Human Verification')).toBeInTheDocument();
  });

  it('does not render dialog when open is false', () => {
    render(CaptchaModal, { props: { ...defaultProps, open: false } });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('calls createChallenge on mount when open is true', async () => {
    render(CaptchaModal, { props: defaultProps });

    await waitFor(() => {
      expect(createChallenge).toHaveBeenCalledOnce();
    });
  });

  it('shows loading spinner while challenge is loading', () => {
    // Delay resolution so we can check loading state
    vi.mocked(createChallenge).mockReturnValue(new Promise(() => {}));

    render(CaptchaModal, { props: defaultProps });

    expect(screen.getByLabelText('Loading challenge')).toBeInTheDocument();
  });

  it('renders the challenge image as inline base64 after loading', async () => {
    render(CaptchaModal, { props: defaultProps });

    await waitFor(() => {
      const img = screen.getByAltText(/click the correct answer/i);
      expect(img).toBeInTheDocument();
      expect(img).toHaveAttribute('src', expect.stringContaining('data:image/png;base64,'));
    });
  });

  it('renders "New challenge" button', async () => {
    render(CaptchaModal, { props: defaultProps });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /new challenge/i })).toBeInTheDocument();
    });
  });

  // --- Correct answer flow ---

  it('calls verifyChallenge with challengeToken and click coordinates when image is clicked', async () => {
    vi.mocked(verifyChallenge).mockResolvedValue({ token: 'signed-token-abc' });

    render(CaptchaModal, { props: defaultProps });

    const img = await screen.findByAltText(/click the correct answer/i);

    Object.defineProperty(img, 'naturalWidth', { value: 400 });
    Object.defineProperty(img, 'naturalHeight', { value: 160 });
    Object.defineProperty(img, 'getBoundingClientRect', {
      value: () => ({ left: 0, top: 0, width: 400, height: 160 }),
    });

    await fireEvent.click(img, { clientX: 100, clientY: 120 });

    await waitFor(() => {
      expect(verifyChallenge).toHaveBeenCalledWith('encrypted-challenge-token', 100, 120);
    });
  });

  it('saves the token and calls onSuccess after correct answer', async () => {
    const onSuccess = vi.fn();
    vi.mocked(verifyChallenge).mockResolvedValue({ token: 'signed-token-abc' });

    render(CaptchaModal, { props: { open: true, onSuccess } });

    const img = await screen.findByAltText(/click the correct answer/i);
    Object.defineProperty(img, 'naturalWidth', { value: 400 });
    Object.defineProperty(img, 'naturalHeight', { value: 160 });
    Object.defineProperty(img, 'getBoundingClientRect', {
      value: () => ({ left: 0, top: 0, width: 400, height: 160 }),
    });

    await fireEvent.click(img, { clientX: 100, clientY: 120 });

    await waitFor(() => {
      expect(saveToken).toHaveBeenCalledWith('signed-token-abc');
      expect(onSuccess).toHaveBeenCalledOnce();
    });
  });

  // --- Wrong answer flow ---

  it('shows error message on wrong answer', async () => {
    vi.mocked(verifyChallenge).mockRejectedValue({ status: 400, detail: 'Wrong answer' });
    // Make the reload hang so the error message stays visible while asserting
    vi.mocked(createChallenge).mockResolvedValueOnce({
      challengeToken: 'encrypted-challenge-token',
      imageBase64: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    }).mockReturnValueOnce(new Promise(() => {}));

    render(CaptchaModal, { props: defaultProps });

    const img = await screen.findByAltText(/click the correct answer/i);
    Object.defineProperty(img, 'naturalWidth', { value: 400 });
    Object.defineProperty(img, 'naturalHeight', { value: 160 });
    Object.defineProperty(img, 'getBoundingClientRect', {
      value: () => ({ left: 0, top: 0, width: 400, height: 160 }),
    });

    await fireEvent.click(img, { clientX: 5, clientY: 5 });

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/wrong answer/i);
    });
  });

  it('loads a new challenge after wrong answer', async () => {
    vi.mocked(verifyChallenge).mockRejectedValue({ status: 400 });

    render(CaptchaModal, { props: defaultProps });

    const img = await screen.findByAltText(/click the correct answer/i);
    Object.defineProperty(img, 'naturalWidth', { value: 400 });
    Object.defineProperty(img, 'naturalHeight', { value: 160 });
    Object.defineProperty(img, 'getBoundingClientRect', {
      value: () => ({ left: 0, top: 0, width: 400, height: 160 }),
    });

    await fireEvent.click(img, { clientX: 5, clientY: 5 });

    await waitFor(() => {
      expect(createChallenge).toHaveBeenCalledTimes(2);
    });
  });

  // --- New challenge button ---

  it('loads a new challenge when "New challenge" is clicked', async () => {
    render(CaptchaModal, { props: defaultProps });

    await screen.findByAltText(/click the correct answer/i);

    await fireEvent.click(screen.getByRole('button', { name: /new challenge/i }));

    await waitFor(() => {
      expect(createChallenge).toHaveBeenCalledTimes(2);
    });
  });
});
