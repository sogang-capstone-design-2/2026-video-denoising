export type Mode = "general" | "lowlight";

export type ProcessingIntensity = "low" | "medium" | "high";

export interface UploadSession {
  mode: Mode;
  intensity: ProcessingIntensity;
  fileName: string;
  fileSize: number;
  /** data URL for image preview; null for non-image files */
  previewUrl: string | null;
}

export interface ResultSession {
  jobId: string;
  mode: Mode;
  fileName: string;
  beforeUrl: string;
  /** lowlight 모드에서 denoised 처리 전이면 null */
  afterUrl: string | null;
}
