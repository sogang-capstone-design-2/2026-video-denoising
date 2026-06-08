// Module-level store to pass a File object across client-side page navigations.
// sessionStorage cannot hold binary blobs, so we keep it in memory.

let pendingFile: File | null = null;

export function setFile(file: File): void {
  pendingFile = file;
}

export function getFile(): File | null {
  return pendingFile;
}

export function clearFile(): void {
  pendingFile = null;
}
