/**
 * Calls the FairNSquare REST API (`/api/...`).
 *
 * @typedef {{type: string, title: string, status: number, detail: string}} ApiError
 *   The problem details the server sends on error. A network failure gives status 0.
 */

const BASE_URL = '/api';

/**
 * Sends a request to the API and returns the JSON body (undefined for 204 No Content).
 *
 * @param {string} endpoint the path after /api, e.g. '/admin/stats'
 * @param {RequestInit} [options] fetch options; headers are merged with the JSON content type
 * @returns {Promise<any>}
 * @throws {ApiError} when the server answers with an error, or cannot be reached
 */
export async function apiRequest(endpoint, options = {}) {
  let response;
  try {
    response = await fetch(BASE_URL + endpoint, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...options.headers },
    });
  } catch {
    throw {
      type: 'https://fairnsquare.app/errors/network-error',
      title: 'Network Error',
      status: 0,
      detail: 'Unable to connect to the server. Please check your connection and try again.',
    };
  }

  if (!response.ok) {
    let error;
    try {
      error = await response.json();
    } catch {
      error = {
        type: 'https://fairnsquare.app/errors/server-error',
        title: 'Server Error',
        status: response.status,
        detail: `Server returned status ${response.status}. Please try again later.`,
      };
    }
    throw error;
  }

  if (response.status === 204) {
    return undefined;
  }
  return response.json();
}
