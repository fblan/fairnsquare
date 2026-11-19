<script lang="ts">
  // The participants page is a web component now (/biz/participants/participants-page.js, #174); this route only shows it.
  import { route } from '$lib/router';
</script>

<fns-participants-page split-id={route.params.splitId}></fns-participants-page>
