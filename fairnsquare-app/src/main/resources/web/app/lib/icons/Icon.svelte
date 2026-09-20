<script lang="ts">
  let { size = 24, color = 'currentColor', strokeWidth = 2, class: className = '', iconNode = [], ...rest }: {
    size?: number;
    color?: string;
    strokeWidth?: number;
    class?: string;
    iconNode?: [string, Record<string, string>][];
    [key: string]: unknown;
  } = $props();
</script>
<svg
  xmlns="http://www.w3.org/2000/svg"
  width={size}
  height={size}
  viewBox="0 0 24 24"
  fill="none"
  stroke={color}
  stroke-width={strokeWidth}
  stroke-linecap="round"
  stroke-linejoin="round"
  aria-hidden="true"
  class={className}
  {...rest}
>
  {#each iconNode as [tag, attrs]}
    <svelte:element this={tag} {...attrs}/>
  {/each}
</svg>
