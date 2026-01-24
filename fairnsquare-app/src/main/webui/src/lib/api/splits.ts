/**
 * Splits API client
 */

import { apiRequest } from './client';

export interface CreateSplitRequest {
  name: string;
}

export interface Participant {
  id: string;
  name: string;
  nights: number;
}

export interface Expense {
  id: string;
  description: string;
  amount: number;
  payerId: string;
  splitMode: 'BY_NIGHT' | 'EQUAL' | 'FREE';
  createdAt: string;
}

export interface Split {
  id: string;
  name: string;
  createdAt: string;
  participants: Participant[];
  expenses: Expense[];
}

/**
 * Creates a new split.
 * @param request The create split request containing the name
 * @returns The created split with generated ID and timestamp
 */
export async function createSplit(request: CreateSplitRequest): Promise<Split> {
  return apiRequest<Split>('/splits', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Gets a split by ID.
 * @param splitId The split identifier
 * @returns The split if found
 */
export async function getSplit(splitId: string): Promise<Split> {
  return apiRequest<Split>(`/splits/${splitId}`);
}