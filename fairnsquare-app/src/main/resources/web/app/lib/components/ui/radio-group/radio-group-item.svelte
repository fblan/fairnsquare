<script lang="ts">
  import { getContext } from 'svelte';

  let {
    value,
    id,
    class: className = '',
  }: {
    value: string;
    id?: string;
    class?: string;
  } = $props();

  const radioGroup = getContext<{
    getValue: () => string;
    setValue: (value: string) => void;
  }>('radioGroup');

  const checked = $derived(radioGroup.getValue() === value);

  function handleClick() {
    radioGroup.setValue(value);
  }
</script>

<input
  type="radio"
  {id}
  {value}
  {checked}
  onclick={handleClick}
  class="h-4 w-4 rounded-full border-2 border-primary text-primary focus:ring-2 focus:ring-primary focus:ring-offset-2 {className}"
/>
