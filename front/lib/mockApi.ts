/**
 * Mock API — replace these with real fetch/axios calls once the backend is ready.
 * All functions simulate network latency and return fake data.
 */

const delay = (ms: number) => new Promise((r) => setTimeout(r, ms));

export interface UploadProgress {
  phase: "uploading" | "processing" | "finalizing";
  percent: number;
}

/**
 * Simulates a full upload + processing job.
 * Calls onProgress as the job advances; resolves when done.
 */
export async function runMockJob(
  onProgress: (p: UploadProgress) => void
): Promise<void> {
  // Phase 1: upload (0 → 30%)
  for (let i = 0; i <= 30; i += 5) {
    onProgress({ phase: "uploading", percent: i });
    await delay(200);
  }
  // Phase 2: processing (30 → 90%)
  for (let i = 35; i <= 90; i += 5) {
    onProgress({ phase: "processing", percent: i });
    await delay(300);
  }
  // Phase 3: finalizing (90 → 100%)
  for (let i = 92; i <= 100; i += 2) {
    onProgress({ phase: "finalizing", percent: i });
    await delay(150);
  }
}

/**
 * TODO: Replace with POST /api/denoise/general or /api/denoise/lowlight
 */
export async function uploadFile(
  _file: File,
  _mode: string,
  onProgress: (p: UploadProgress) => void
): Promise<string> {
  await runMockJob(onProgress);
  return "mock-job-" + Date.now();
}
