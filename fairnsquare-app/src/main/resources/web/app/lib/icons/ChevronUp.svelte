<script lang="ts">
  import Icon from './Icon.svelte';
  const iconNode: [string, Record<string, string>][] = [["path", { "d": "m18 15-6-6-6 6" }]];
  let rest = $props();
</script>
<Icon {iconNode} {...rest} />
