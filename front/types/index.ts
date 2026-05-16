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
  mode: Mode;
  fileName: string;
  beforeUrl: string;
  afterUrl: string;
}
