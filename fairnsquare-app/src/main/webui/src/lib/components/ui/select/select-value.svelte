<script lang="ts">
  import { getContext } from 'svelte';

  let {
    placeholder = 'Select an option',
  }: {
    placeholder?: string;
  } = $props();

  const select = getContext<{
    getSelected: () => { value: string; label: string };
  }>('select');

  const displayValue = $derived(select.getSelected().label || placeholder);
</script>

<span class="truncate">
  {displayValue}
</span>
