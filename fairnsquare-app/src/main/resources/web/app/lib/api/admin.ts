/**
 * Admin API client
 */

import { apiRequest } from './client';

export const ADMIN_TOKEN_HEADER = 'X-Admin-Token';

export interface AdminSplitSummary {
  idHash: string;
  createdAt: string;
  updatedAt: string;
  participantCount: number;
  expenseCount: number;
  expenseTypes: string[];
}

export interface AdminStats {
  totalSplits: number;
  splitCountLimit: number;
  usedStorageBytes: number;
  maxStorageBytes: number;
  lastUpdated: string | null;
  splits: AdminSplitSummary[];
}

export async function getAdminStats(token: string): Promise<AdminStats> {
  return apiRequest<AdminStats>('/admin/stats', {
    headers: { [ADMIN_TOKEN_HEADER]: token },
  });
}
