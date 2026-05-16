import type { UploadSession, ResultSession } from "@/types";

const UPLOAD_KEY = "nvision_upload";
const RESULT_KEY = "nvision_result";

export function saveUploadSession(data: UploadSession): void {
  sessionStorage.setItem(UPLOAD_KEY, JSON.stringify(data));
}

export function loadUploadSession(): UploadSession | null {
  const raw = sessionStorage.getItem(UPLOAD_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UploadSession;
  } catch {
    return null;
  }
}

export function saveResultSession(data: ResultSession): void {
  sessionStorage.setItem(RESULT_KEY, JSON.stringify(data));
}

export function loadResultSession(): ResultSession | null {
  const raw = sessionStorage.getItem(RESULT_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as ResultSession;
  } catch {
    return null;
  }
}

export function clearSessions(): void {
  sessionStorage.removeItem(UPLOAD_KEY);
  sessionStorage.removeItem(RESULT_KEY);
}
