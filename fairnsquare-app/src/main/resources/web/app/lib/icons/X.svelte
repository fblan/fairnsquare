<script lang="ts">
  import Icon from './Icon.svelte';
  const iconNode: [string, Record<string, string>][] = [["path", { "d": "M18 6 6 18" }], ["path", { "d": "m6 6 12 12" }]];
  let rest = $props();
</script>
<Icon {iconNode} {...rest} />
