<script lang="ts">
  // FairNSquare - Root Application Component
  // Router configured in Story 2.3

  import { Router } from 'sv-router';
  // Import router config to initialize routes
  import '$lib/router';
  import Toaster from '$lib/components/ui/toast/Toaster.svelte';
</script>

<main class="min-h-screen bg-background">
  <div class="max-w-content mx-auto px-4 py-6">
    <Router />
  </div>
</main>
<Toaster />
