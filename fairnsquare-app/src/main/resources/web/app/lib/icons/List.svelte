<script lang="ts">
  import Icon from './Icon.svelte';
  const iconNode: [string, Record<string, string>][] = [["path", { "d": "M3 5h.01" }], ["path", { "d": "M3 12h.01" }], ["path", { "d": "M3 19h.01" }], ["path", { "d": "M8 5h13" }], ["path", { "d": "M8 12h13" }], ["path", { "d": "M8 19h13" }]];
  let rest = $props();
</script>
<Icon {iconNode} {...rest} />
