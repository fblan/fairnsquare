<script lang="ts">
  // Home page - Landing / Create Split
  // Using Svelte 5 runes for state management
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import * as Card from '$lib/components/ui/card';
  import { Label } from '$lib/components/ui/label';

  let splitName = $state('');

  function handleCreateSplit() {
    // Will be implemented in Epic 2
    console.log('Creating split:', splitName);
  }
</script>

<div class="space-y-6">
  <!-- Header -->
  <header class="text-center py-8">
    <h1 class="text-2xl font-bold text-foreground">FairNSquare</h1>
    <p class="text-muted-foreground mt-2">Split expenses fairly with friends</p>
  </header>

  <!-- Create Split Form -->
  <Card.Root>
    <Card.Header>
      <Card.Title>Create a New Split</Card.Title>
    </Card.Header>
    <Card.Content>
      <form onsubmit={(e) => { e.preventDefault(); handleCreateSplit(); }} class="space-y-4">
        <div class="space-y-2">
          <Label for="splitName">Split Name</Label>
          <Input
            type="text"
            id="splitName"
            bind:value={splitName}
            placeholder="e.g., Bordeaux Weekend 2026"
          />
        </div>

        <Button
          type="submit"
          disabled={!splitName.trim()}
          class="w-full"
        >
          Create Split
        </Button>
      </form>
    </Card.Content>
  </Card.Root>

  <!-- Info Section -->
  <section class="text-center text-muted-foreground text-sm">
    <p>No account needed. Create a split and share the link!</p>
  </section>
</div>