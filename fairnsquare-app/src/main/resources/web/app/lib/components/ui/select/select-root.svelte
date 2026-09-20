<script lang="ts">
  import { setContext } from 'svelte';

  let {
    selected = $bindable({ value: '', label: '' }),
    onSelectedChange,
    children,
  }: {
    selected?: { value: string; label: string };
    onSelectedChange?: (item: { value: string; label: string } | null) => void;
    children?: any;
  } = $props();

  let isOpen = $state(false);

  setContext('select', {
    getSelected: () => selected,
    isOpen: () => isOpen,
    setOpen: (open: boolean) => {
      isOpen = open;
    },
    select: (value: string, label: string) => {
      selected = { value, label };
      isOpen = false;
      onSelectedChange?.({ value, label });
    },
  });
</script>

<div class="relative">
  {@render children?.()}
</div>
