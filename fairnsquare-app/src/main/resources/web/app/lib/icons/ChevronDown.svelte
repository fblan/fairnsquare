<script lang="ts">
  import Icon from './Icon.svelte';
  const iconNode: [string, Record<string, string>][] = [["path", { "d": "m6 9 6 6 6-6" }]];
  let rest = $props();
</script>
<Icon {iconNode} {...rest} />
