<script lang="ts">
  // The settlement page is a web component now (/biz/settlement/settlement-page.js, #176); this route only shows it.
  import { route } from '$lib/router';
</script>

<fns-settlement-page split-id={route.params.splitId}></fns-settlement-page>
