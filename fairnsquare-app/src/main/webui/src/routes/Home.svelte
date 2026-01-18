<script lang="ts">
  // Home page - Landing / Create Split
  // Using Svelte 5 runes for state management

  let splitName = $state('');

  function handleCreateSplit() {
    // Will be implemented in Epic 2
    console.log('Creating split:', splitName);
  }
</script>

<div class="space-y-6">
  <!-- Header -->
  <header class="text-center py-8">
    <h1 class="text-2xl font-bold text-text-primary">FairNSquare</h1>
    <p class="text-text-muted mt-2">Split expenses fairly with friends</p>
  </header>

  <!-- Create Split Form -->
  <section class="bg-surface rounded-card p-card shadow-sm border border-border">
    <h2 class="text-lg font-semibold text-text-primary mb-4">Create a New Split</h2>

    <form onsubmit={(e) => { e.preventDefault(); handleCreateSplit(); }} class="space-y-4">
      <div>
        <label for="splitName" class="block text-sm font-medium text-text-primary mb-1">
          Split Name
        </label>
        <input
          type="text"
          id="splitName"
          bind:value={splitName}
          placeholder="e.g., Bordeaux Weekend 2026"
          class="w-full px-3 py-2 border border-border rounded-card bg-surface text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary"
        />
      </div>

      <button
        type="submit"
        disabled={!splitName.trim()}
        class="w-full bg-primary text-white font-medium py-3 rounded-card hover:bg-primary-dark disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
      >
        Create Split
      </button>
    </form>
  </section>

  <!-- Info Section -->
  <section class="text-center text-text-muted text-sm">
    <p>No account needed. Create a split and share the link!</p>
  </section>
</div>