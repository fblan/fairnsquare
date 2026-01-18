<script lang="ts">
  /**
   * Reusable Button component
   * Supports primary, secondary, and danger variants
   * Uses Svelte 5 $props() for prop handling
   */

  interface Props {
    variant?: 'primary' | 'secondary' | 'danger';
    disabled?: boolean;
    type?: 'button' | 'submit' | 'reset';
    class?: string;
    onclick?: () => void;
    children?: import('svelte').Snippet;
  }

  let {
    variant = 'primary',
    disabled = false,
    type = 'button',
    class: className = '',
    onclick,
    children,
  }: Props = $props();

  const baseClasses = 'min-h-touch px-4 py-2 rounded-card font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2';

  const variantClasses = {
    primary: 'bg-primary text-white hover:bg-primary-dark focus:ring-primary disabled:opacity-50',
    secondary: 'bg-secondary text-white hover:bg-secondary-light focus:ring-secondary disabled:opacity-50',
    danger: 'bg-danger text-white hover:bg-danger-light focus:ring-danger disabled:opacity-50',
  };
</script>

<button
  {type}
  {disabled}
  {onclick}
  class="{baseClasses} {variantClasses[variant]} {className}"
>
  {#if children}
    {@render children()}
  {/if}
</button>