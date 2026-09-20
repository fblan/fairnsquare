<script lang="ts">
	import type { HTMLInputAttributes } from "svelte/elements";
	import { cn, type WithElementRef } from "$lib/utils.js";

	type Props = WithElementRef<Omit<HTMLInputAttributes, "type">> & {
		checked?: boolean;
	};

	let {
		ref = $bindable(null),
		checked = $bindable(false),
		class: className,
		...restProps
	}: Props = $props();
</script>

<input
	bind:this={ref}
	type="checkbox"
	bind:checked
	class={cn(
		"h-4 w-4 shrink-0 rounded border border-input bg-background shadow-xs transition-colors",
		"checked:bg-primary checked:border-primary checked:accent-primary",
		"focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
		"disabled:cursor-not-allowed disabled:opacity-50",
		className
	)}
	{...restProps}
/>