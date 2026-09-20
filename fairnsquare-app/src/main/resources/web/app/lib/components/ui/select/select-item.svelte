<script lang="ts">
  import { getContext } from 'svelte';

  let {
    value,
    label,
    children,
  }: {
    value: string;
    label?: string;
    children?: any;
  } = $props();

  const select = getContext<{
    select: (value: string, label: string) => void;
  }>('select');

  function handleClick() {
    // Use explicit label prop, otherwise try to extract from children, fallback to value
    const displayLabel = label || (typeof children === 'string' ? children : value);
    select.select(value, displayLabel);
  }
</script>

<button
  type="button"
  onclick={handleClick}
  class="w-full px-2 py-1.5 text-left text-sm rounded hover:bg-accent cursor-pointer"
>
  {@render children?.()}
</button>
