/**
 * API Client - Fetch wrapper with error handling
 * Implementation will be added in Epic 2
 */

const BASE_URL = '/api';

export interface ApiError {
  type: string;
  title: string;
  status: number;
  detail: string;
}

export async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${BASE_URL}${endpoint}`;

  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });

  if (!response.ok) {
    const error: ApiError = await response.json();
    throw error;
  }

  return response.json();
}