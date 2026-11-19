<script lang="ts">
  // The expenses page is a web component now (/biz/expenses/expenses-page.js, #170); this route only shows it.
  import { route } from '$lib/router';
</script>

<fns-expenses-page split-id={route.params.splitId}></fns-expenses-page>
