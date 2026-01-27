<script lang="ts">
  import { getContext } from 'svelte';

  let {
    children,
  }: {
    children?: any;
  } = $props();

  const select = getContext<{
    isOpen: () => boolean;
  }>('select');

  const isOpen = $derived(select.isOpen());
</script>

{#if isOpen}
  <div class="absolute top-full left-0 mt-1 w-full z-50 rounded-md border bg-popover p-1 shadow-md">
    {@render children?.()}
  </div>
{/if}
