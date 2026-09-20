<script lang="ts">
  import Icon from './Icon.svelte';
  const iconNode: [string, Record<string, string>][] = [["path", { "d": "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" }], ["circle", { "cx": "12", "cy": "7", "r": "4" }]];
  let rest = $props();
</script>
<Icon {iconNode} {...rest} />
