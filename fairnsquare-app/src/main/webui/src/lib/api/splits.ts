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