/**
 * Splits API client
 */

import { apiRequest } from './client';

export interface CreateSplitRequest {
  name: string;
}

export interface AddParticipantRequest {
  name: string;
  nights: number;
}

export interface UpdateParticipantRequest {
  name: string;
  nights: number;
}

export interface Participant {
  id: string;
  name: string;
  nights: number;
}

export type SplitMode = 'BY_NIGHT' | 'EQUAL' | 'FREE';

export interface Share {
  participantId: string;
  amount: number;
}

export interface Expense {
  id: string;
  description: string;
  amount: number;
  payerId: string;
  splitMode: SplitMode;
  createdAt: string;
  shares: Share[];
}

export interface AddExpenseRequest {
  amount: number;
  description: string;
  payerId: string;
  splitMode: SplitMode;
}

export interface UpdateExpenseRequest {
  amount: number;
  description: string;
  payerId: string;
  splitMode: SplitMode;
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

/**
 * Adds a participant to a split.
 * @param splitId The split identifier
 * @param request The add participant request
 * @returns The created participant
 */
export async function addParticipant(
  splitId: string,
  request: AddParticipantRequest
): Promise<Participant> {
  return apiRequest<Participant>(`/splits/${splitId}/participants`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Updates an existing participant in a split.
 * @param splitId The split identifier
 * @param participantId The participant identifier
 * @param request The update participant request
 * @returns The updated participant
 */
export async function updateParticipant(
  splitId: string,
  participantId: string,
  request: UpdateParticipantRequest
): Promise<Participant> {
  return apiRequest<Participant>(`/splits/${splitId}/participants/${participantId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

/**
 * Deletes a participant from a split.
 * @param splitId The split identifier
 * @param participantId The participant identifier
 * @throws Error with message if participant has associated expenses (409 Conflict)
 */
export async function deleteParticipant(
  splitId: string,
  participantId: string
): Promise<void> {
  await apiRequest<void>(`/splits/${splitId}/participants/${participantId}`, {
    method: 'DELETE',
  });
}

/**
 * Adds an expense to a split.
 * @param splitId The split identifier
 * @param request The add expense request
 * @returns The created expense with calculated shares
 */
export async function addExpense(
  splitId: string,
  request: AddExpenseRequest
): Promise<Expense> {
  return apiRequest<Expense>(`/splits/${splitId}/expenses`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Updates an existing expense in a split.
 * @param splitId The split identifier
 * @param expenseId The expense identifier
 * @param request The update expense request
 * @returns The updated expense with recalculated shares
 */
export async function updateExpense(
  splitId: string,
  expenseId: string,
  request: UpdateExpenseRequest
): Promise<Expense> {
  return apiRequest<Expense>(`/splits/${splitId}/expenses/${expenseId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

/**
 * Deletes an expense from a split.
 * @param splitId The split identifier
 * @param expenseId The expense identifier
 */
export async function deleteExpense(
  splitId: string,
  expenseId: string
): Promise<void> {
  await apiRequest<void>(`/splits/${splitId}/expenses/${expenseId}`, {
    method: 'DELETE',
  });
}