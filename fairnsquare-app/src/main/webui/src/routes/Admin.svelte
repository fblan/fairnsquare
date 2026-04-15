<script lang="ts">
  // Admin page — password-protected stats view
  // Issue #107: Not linked from the main navigation
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import * as Card from '$lib/components/ui/card';
  import { getAdminStats, type AdminStats } from '$lib/api/admin';
  import type { ApiError } from '$lib/api/client';

  const SESSION_KEY = 'admin_token';

  let token = $state(sessionStorage.getItem(SESSION_KEY) ?? '');
  let tokenInput = $state('');
  let stats = $state<AdminStats | null>(null);
  let isLoading = $state(false);
  let errorMessage = $state<string | null>(null);

  // If a token is stored in sessionStorage, try to load stats immediately
  $effect(() => {
    if (token) {
      doLoadStats(token);
    }
  });

  async function handleLogin() {
    if (!tokenInput.trim()) return;
    errorMessage = null;
    await doLoadStats(tokenInput.trim());
  }

  async function doLoadStats(t: string) {
    isLoading = true;
    errorMessage = null;
    try {
      const result = await getAdminStats(t);
      stats = result;
      token = t;
      sessionStorage.setItem(SESSION_KEY, t);
    } catch (err) {
      const apiError = err as ApiError;
      stats = null;
      if (apiError.status === 401) {
        errorMessage = 'Invalid password.';
        token = '';
        sessionStorage.removeItem(SESSION_KEY);
      } else if (apiError.status === 503) {
        errorMessage = 'Admin access is not configured on this server.';
      } else {
        errorMessage = apiError.detail || 'Failed to load admin stats.';
      }
    } finally {
      isLoading = false;
    }
  }

  function handleLogout() {
    stats = null;
    token = '';
    tokenInput = '';
    sessionStorage.removeItem(SESSION_KEY);
  }

  function formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString();
  }

  function formatBytes(bytes: number): string {
    if (bytes >= 1024 * 1024 * 1024) return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
    if (bytes >= 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
    if (bytes >= 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return bytes + ' B';
  }

  // Oldest-created: first 5 splits sorted by createdAt asc
  const oldestCreated = $derived(
    stats ? [...stats.splits].sort((a, b) => a.createdAt.localeCompare(b.createdAt)).slice(0, 5) : []
  );

  // Oldest-updated: first 5 splits sorted by updatedAt asc (least recently touched)
  const oldestUpdated = $derived(
    stats ? [...stats.splits].sort((a, b) => a.updatedAt.localeCompare(b.updatedAt)).slice(0, 5) : []
  );

  // Most-recently-updated: first 5 splits sorted by updatedAt desc
  const mostRecentlyUpdated = $derived(
    stats ? [...stats.splits].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt)).slice(0, 5) : []
  );
</script>

<div class="flex flex-col items-center space-y-6 w-full max-w-[900px] mx-auto py-8 px-4">
  <header class="text-center">
    <h1 class="text-2xl font-bold text-primary">Admin</h1>
    <p class="text-muted-foreground mt-1 text-sm">FairNSquare administration</p>
  </header>

  {#if !stats}
    <!-- Password form -->
    <div class="w-full max-w-[400px]">
      <Card.Root>
        <Card.Header>
          <Card.Title>Sign in</Card.Title>
        </Card.Header>
        <Card.Content>
          <form onsubmit={(e) => { e.preventDefault(); handleLogin(); }} class="space-y-4">
            <div class="space-y-2">
              <Label for="admin-password">Password</Label>
              <Input
                id="admin-password"
                type="password"
                bind:value={tokenInput}
                placeholder="Admin password"
                disabled={isLoading}
                class="min-h-[44px]"
                autocomplete="current-password"
              />
            </div>
            {#if errorMessage}
              <p class="text-sm text-destructive">{errorMessage}</p>
            {/if}
            <Button type="submit" disabled={isLoading || !tokenInput.trim()} class="w-full min-h-[44px]">
              {isLoading ? 'Signing in…' : 'Sign in'}
            </Button>
          </form>
        </Card.Content>
      </Card.Root>
    </div>

  {:else}
    <!-- Stats dashboard -->
    <div class="w-full flex justify-end">
      <Button variant="outline" onclick={handleLogout} class="min-h-[44px]">Sign out</Button>
    </div>

    <!-- Summary cards -->
    <div class="w-full grid grid-cols-2 sm:grid-cols-3 gap-3">
      <Card.Root>
        <Card.Content class="py-4 text-center">
          <p class="text-3xl font-bold text-primary">{stats.totalSplits} <span class="text-lg font-normal text-muted-foreground">/ {stats.splitCountLimit}</span></p>
          <p class="text-sm text-muted-foreground mt-1">Total splits</p>
        </Card.Content>
      </Card.Root>
      <Card.Root>
        <Card.Content class="py-4 text-center">
          <p class="text-xl font-bold text-primary">{formatBytes(stats.usedStorageBytes)} <span class="text-sm font-normal text-muted-foreground">/ {formatBytes(stats.maxStorageBytes)}</span></p>
          <p class="text-sm text-muted-foreground mt-1">Storage used</p>
        </Card.Content>
      </Card.Root>
      <Card.Root class="col-span-2 sm:col-span-1">
        <Card.Content class="py-4">
          <p class="text-xs text-muted-foreground uppercase tracking-wide">Last updated</p>
          <p class="text-sm font-medium mt-1">{formatDate(stats.lastUpdated)}</p>
        </Card.Content>
      </Card.Root>
    </div>

    <!-- Oldest created -->
    <div class="w-full">
      <h2 class="text-sm font-semibold text-muted-foreground uppercase tracking-wide mb-2">Oldest created splits</h2>
      <Card.Root>
        <Card.Content class="py-0">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b">
                <th class="text-left py-2 px-3 font-medium text-muted-foreground">ID hash</th>
                <th class="text-left py-2 px-3 font-medium text-muted-foreground">Created</th>
              </tr>
            </thead>
            <tbody>
              {#each oldestCreated as s (s.idHash)}
                <tr class="border-b last:border-0">
                  <td class="py-2 px-3 font-mono text-xs">{s.idHash}</td>
                  <td class="py-2 px-3 text-muted-foreground">{formatDate(s.createdAt)}</td>
                </tr>
              {/each}
            </tbody>
          </table>
        </Card.Content>
      </Card.Root>
    </div>

    <!-- Oldest updated -->
    <div class="w-full">
      <h2 class="text-sm font-semibold text-muted-foreground uppercase tracking-wide mb-2">Oldest updated splits</h2>
      <Card.Root>
        <Card.Content class="py-0">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b">
                <th class="text-left py-2 px-3 font-medium text-muted-foreground">ID hash</th>
                <th class="text-left py-2 px-3 font-medium text-muted-foreground">Last updated</th>
                <th class="text-left py-2 px-3 font-medium text-muted-foreground">Created</th>
              </tr>
            </thead>
            <tbody>
              {#each oldestUpdated as s (s.idHash)}
                <tr class="border-b last:border-0">
                  <td class="py-2 px-3 font-mono text-xs">{s.idHash}</td>
                  <td class="py-2 px-3 text-muted-foreground">{formatDate(s.updatedAt)}</td>
                  <td class="py-2 px-3 text-muted-foreground">{formatDate(s.createdAt)}</td>
                </tr>
              {/each}
            </tbody>
          </table>
        </Card.Content>
      </Card.Root>
    </div>

    <!-- Most recently updated -->
    <div class="w-full">
      <h2 class="text-sm font-semibold text-muted-foreground uppercase tracking-wide mb-2">Most recently updated splits</h2>
      <Card.Root>
        <Card.Content class="py-0">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b">
                <th class="text-left py-2 px-3 font-medium text-muted-foreground">ID hash</th>
                <th class="text-left py-2 px-3 font-medium text-muted-foreground">Last updated</th>
                <th class="text-left py-2 px-3 font-medium text-muted-foreground">Created</th>
              </tr>
            </thead>
            <tbody>
              {#each mostRecentlyUpdated as s (s.idHash)}
                <tr class="border-b last:border-0">
                  <td class="py-2 px-3 font-mono text-xs">{s.idHash}</td>
                  <td class="py-2 px-3 text-muted-foreground">{formatDate(s.updatedAt)}</td>
                  <td class="py-2 px-3 text-muted-foreground">{formatDate(s.createdAt)}</td>
                </tr>
              {/each}
            </tbody>
          </table>
        </Card.Content>
      </Card.Root>
    </div>

  {/if}
</div>
