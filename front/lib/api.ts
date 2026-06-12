import type { ProcessingIntensity } from "@/types";

export type JobPhase = "queued" | "uploading" | "processing" | "finalizing" | "done" | "failed";

export interface JobStatus {
  phase: JobPhase;
  percent: number;
  resultUrl?: string;
  noisyImageUrl?: string;
  error?: string;
}

const INTENSITY_TO_SIGMA: Record<ProcessingIntensity, number> = {
  low: 15,
  medium: 25,
  high: 40,
};

// 백엔드 Base URL (프록시 거치지 않고 직접)
const BACKEND_URL = "http://localhost:8080";

export async function submitJob(
  file: File,
  mode: "general" | "lowlight",
  intensity: ProcessingIntensity,
  addNoise = false,
  compare = false
): Promise<string> {
  const form = new FormData();
  form.append("file", file);
  form.append("mode", mode);

  if (mode === "general") {
    form.append("noiseSigma", String(INTENSITY_TO_SIGMA[intensity]));
    form.append("addNoise", String(addNoise));
    form.append("compare", String(compare));
  }

  // 프록시 거치지 않고 백엔드에 직접 요청
  const res = await fetch(`${BACKEND_URL}/api/jobs`, {
    method: "POST",
    body: form,
  });
  if (!res.ok) {
    const msg = await res.text().catch(() => res.statusText);
    throw new Error(`업로드 실패 (${res.status}): ${msg}`);
  }
  const data = await res.json();
  return data.jobId as string;
}

export async function getJobStatus(jobId: string): Promise<JobStatus> {
  const res = await fetch(`${BACKEND_URL}/api/jobs/${jobId}/status`);
  if (!res.ok) throw new Error(`상태 조회 실패 (${res.status})`);
  return res.json() as Promise<JobStatus>;
}

export async function pollUntilDone(
  jobId: string,
  onProgress: (status: JobStatus) => void,
  intervalMs = 1500
): Promise<void> {
  return new Promise((resolve, reject) => {
    const tick = async () => {
      try {
        const status = await getJobStatus(jobId);
        onProgress(status);

        if (status.phase === "done") {
          resolve();
        } else if (status.phase === "failed") {
          reject(new Error(status.error ?? "처리 실패"));
        } else {
          setTimeout(tick, intervalMs);
        }
      } catch (err) {
        reject(err);
      }
    };
    tick();
  });
}
