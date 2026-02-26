import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import ParticipantSummaryCard from './ParticipantSummaryCard.svelte';
import type { Participant } from '$lib/api/splits';

describe('ParticipantSummaryCard', () => {
  // --- Default mode (showTitle=true, used in Split dashboard) ---

  it('displays participant count and names sorted alphabetically', () => {
    const participants: Participant[] = [
      { id: 'p2', name: 'Bob', nights: 2, share: 1 },
      { id: 'p1', name: 'Alice', nights: 4, share: 1 },
    ];

    render(ParticipantSummaryCard, { props: { participants } });

    expect(screen.getByText('2 participants')).toBeInTheDocument();
    expect(screen.getByText('Alice (4n), Bob (2n)')).toBeInTheDocument();
  });

  it('displays singular "participant" when only one', () => {
    const participants: Participant[] = [
      { id: 'p1', name: 'Alice', nights: 3, share: 1 },
    ];

    render(ParticipantSummaryCard, { props: { participants } });

    expect(screen.getByText('1 participant')).toBeInTheDocument();
  });

  it('displays 0 participants when empty', () => {
    render(ParticipantSummaryCard, { props: { participants: [] } });

    expect(screen.getByText('0 participants')).toBeInTheDocument();
  });

  it('does not show summary line when no participants', () => {
    render(ParticipantSummaryCard, { props: { participants: [] } });

    expect(screen.queryByText(',')).not.toBeInTheDocument();
  });

  it('shows share suffix when share > 1', () => {
    const participants: Participant[] = [
      { id: 'p1', name: 'Alice', nights: 4, share: 2 },
      { id: 'p2', name: 'Bob', nights: 2, share: 1 },
    ];

    render(ParticipantSummaryCard, { props: { participants } });

    expect(screen.getByText('Alice (4n, ×2), Bob (2n)')).toBeInTheDocument();
  });

  it('shows fractional share suffix', () => {
    const participants: Participant[] = [
      { id: 'p1', name: 'Charlie', nights: 7, share: 2.5 },
    ];

    render(ParticipantSummaryCard, { props: { participants } });

    expect(screen.getByText('Charlie (7n, ×2.5)')).toBeInTheDocument();
  });

  it('formats names with proper case', () => {
    const participants: Participant[] = [
      { id: 'p1', name: 'alice', nights: 3, share: 1 },
    ];

    render(ParticipantSummaryCard, { props: { participants } });

    expect(screen.getByText('Alice (3n)')).toBeInTheDocument();
  });

  // --- No-title mode (showTitle=false, used in Participants page) ---

  it('hides count when showTitle is false', () => {
    const participants: Participant[] = [
      { id: 'p1', name: 'Alice', nights: 4, share: 1 },
    ];

    render(ParticipantSummaryCard, { props: { participants, showTitle: false } });

    expect(screen.queryByText('1 participant')).not.toBeInTheDocument();
    expect(screen.getByText('Alice (4n)')).toBeInTheDocument();
  });

  it('does not render when showTitle is false and no participants', () => {
    render(ParticipantSummaryCard, { props: { participants: [], showTitle: false } });

    expect(screen.queryByText(/\(.*n.*\)/)).not.toBeInTheDocument();
  });
});
