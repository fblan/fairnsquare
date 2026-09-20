<script lang="ts">
  import { getContext } from 'svelte';

  let {
    id,
    class: className = '',
    children,
    ...rest
  }: {
    id?: string;
    class?: string;
    children?: any;
    [key: string]: any;
  } = $props();

  const select = getContext<{
    isOpen: () => boolean;
    setOpen: (open: boolean) => void;
  }>('select');

  function toggle() {
    select.setOpen(!select.isOpen());
  }
</script>

<button
  type="button"
  {id}
  onclick={toggle}
  class="flex w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring {className}"
  {...rest}
>
  {@render children?.()}
</button>
