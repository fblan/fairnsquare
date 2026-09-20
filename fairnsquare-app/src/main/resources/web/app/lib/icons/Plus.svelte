<script lang="ts">
  import Icon from './Icon.svelte';
  const iconNode: [string, Record<string, string>][] = [["path", { "d": "M5 12h14" }], ["path", { "d": "M12 5v14" }]];
  let rest = $props();
</script>
<Icon {iconNode} {...rest} />
