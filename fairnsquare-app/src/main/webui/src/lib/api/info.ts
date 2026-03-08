import { apiRequest } from './client';

export interface AppInfo {
  version: string;
  commit: string;
}

export async function getAppInfo(): Promise<AppInfo> {
  return apiRequest<AppInfo>('/info');
}