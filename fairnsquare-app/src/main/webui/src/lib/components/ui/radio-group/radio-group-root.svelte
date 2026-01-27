<script lang="ts">
  import { setContext } from 'svelte';

  let {
    value = $bindable(''),
    class: className = '',
    children,
  }: {
    value?: string;
    class?: string;
    children?: any;
  } = $props();

  setContext('radioGroup', {
    getValue: () => value,
    setValue: (newValue: string) => {
      value = newValue;
    },
  });
</script>

<div class={className}>
  {@render children?.()}
</div>
