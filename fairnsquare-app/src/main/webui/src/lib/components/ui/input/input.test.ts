import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import Input from './input.svelte';

describe('Input component', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('Select-all on focus for number inputs', () => {
    it('calls select() on the input element when a number input is focused', async () => {
      render(Input, { props: { type: 'number', value: 42 } });

      const input = screen.getByRole('spinbutton') as HTMLInputElement;
      const selectSpy = vi.spyOn(input, 'select');

      await fireEvent.focus(input);
      vi.runAllTimers();

      expect(selectSpy).toHaveBeenCalledOnce();
    });

    it('does not call select() when a text input is focused', async () => {
      render(Input, { props: { type: 'text', value: 'hello' } });

      const input = screen.getByRole('textbox') as HTMLInputElement;
      const selectSpy = vi.spyOn(input, 'select');

      await fireEvent.focus(input);
      vi.runAllTimers();

      expect(selectSpy).not.toHaveBeenCalled();
    });

    it('still calls a parent-provided onfocus handler when focused', async () => {
      const onFocus = vi.fn();
      render(Input, { props: { type: 'number', value: 10, onfocus: onFocus } });

      const input = screen.getByRole('spinbutton') as HTMLInputElement;

      await fireEvent.focus(input);

      expect(onFocus).toHaveBeenCalledOnce();
    });

    it('calls both select() and a parent onfocus for number inputs', async () => {
      const onFocus = vi.fn();
      render(Input, { props: { type: 'number', value: 10, onfocus: onFocus } });

      const input = screen.getByRole('spinbutton') as HTMLInputElement;
      const selectSpy = vi.spyOn(input, 'select');

      await fireEvent.focus(input);
      vi.runAllTimers();

      expect(selectSpy).toHaveBeenCalledOnce();
      expect(onFocus).toHaveBeenCalledOnce();
    });
  });
});