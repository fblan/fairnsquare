<script lang="ts">
  import type { Participant } from '$lib/api/splits';
  import * as Card from '$lib/components/ui/card';

  let {
    participants,
    showTitle = true,
  }: {
    participants: Participant[];
    showTitle?: boolean;
  } = $props();

  const sortedParticipants = $derived(
    participants.slice().sort((a, b) => a.name.localeCompare(b.name))
  );

  function formatName(name: string): string {
    if (!name) return name;
    return name.charAt(0).toUpperCase() + name.slice(1).toLowerCase();
  }

  const summaryText = $derived(
    sortedParticipants
      .map(p => `${formatName(p.name)} (${p.nights}n${p.numberOfPersons > 1 ? `, ${p.numberOfPersons}p` : ''})`)
      .join(', ')
  );
</script>

{#if showTitle || sortedParticipants.length > 0}
  <Card.Root class="w-full border-teal-200 bg-teal-50/50">
    <Card.Content class="py-4">
      {#if showTitle}
        <p class="text-sm text-muted-foreground">Participants</p>
        <p class="text-lg font-semibold">
          {participants.length} {participants.length === 1 ? 'participant' : 'participants'}
        </p>
      {/if}
      {#if sortedParticipants.length > 0}
        <p class="text-sm text-muted-foreground {showTitle ? 'mt-1' : ''}">{summaryText}</p>
      {/if}
    </Card.Content>
  </Card.Root>
{/if}
