/**
 * sv-router configuration
 * Defines all routes for the application
 */

import { createRouter } from 'sv-router';
import Home from '../routes/Home.svelte';
import Split from '../routes/Split.svelte';
import ExpenseList from '../routes/ExpenseList.svelte';

export const { p, navigate, isActive, route } = createRouter({
  '/': Home,
  '/splits/:splitId': Split,
  '/splits/:splitId/expenses': ExpenseList,
});
