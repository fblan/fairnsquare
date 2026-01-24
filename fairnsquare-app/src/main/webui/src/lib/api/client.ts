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

  let response: Response;
  try {
    response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });
  } catch (networkError) {
    // Network failure (no connection, DNS failure, etc.)
    throw {
      type: 'https://fairnsquare.app/errors/network-error',
      title: 'Network Error',
      status: 0,
      detail: 'Unable to connect to the server. Please check your connection and try again.',
    } satisfies ApiError;
  }

  if (!response.ok) {
    let error: ApiError;
    try {
      error = await response.json();
    } catch {
      // Server returned non-JSON error response
      error = {
        type: 'https://fairnsquare.app/errors/server-error',
        title: 'Server Error',
        status: response.status,
        detail: `Server returned status ${response.status}. Please try again later.`,
      };
    }
    throw error;
  }

  return response.json();
}