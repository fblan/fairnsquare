<script lang="ts">
  import { onMount } from 'svelte';
  import { getAppInfo, type AppInfo } from '$lib/api/info';

  let appInfo = $state<AppInfo | null>(null);

  onMount(async () => {
    try {
      appInfo = await getAppInfo();
    } catch {
      // Footer info is non-critical — silently ignore failures
    }
  });
</script>

{#if appInfo}
  <footer class="mt-8 py-4 text-center text-xs text-muted-foreground">
    {appInfo.commit.slice(0, 7)}
  </footer>
{/if}