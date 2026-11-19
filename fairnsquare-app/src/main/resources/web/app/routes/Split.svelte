<script lang="ts">
  // The split dashboard is a web component now (/biz/split/split-page.js, #166); this route only shows it.
  import { route } from '$lib/router';
</script>

<fns-split-page split-id={route.params.splitId}></fns-split-page>
