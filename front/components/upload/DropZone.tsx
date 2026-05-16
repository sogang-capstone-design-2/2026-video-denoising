"use client";

import { useState, useCallback, useRef } from "react";
import type { Mode } from "@/types";

interface DropZoneProps {
  mode: Mode;
  onFileSelected: (file: File) => void;
  selectedFile: File | null;
}

const ACCEPTED: Record<Mode, string[]> = {
  general: [".mp4", ".mov", ".avi"],
  lowlight: [".raw", ".dng", ".nef", ".cr2", ".arw"],
};

const ACCEPTED_MIME: Record<Mode, string> = {
  general: "video/mp4,video/quicktime,video/x-msvideo",
  lowlight: ".raw,.dng,.nef,.cr2,.arw",
};

export default function DropZone({ mode, onFileSelected, selectedFile }: DropZoneProps) {
  const [isDragging, setIsDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragging(false);
      const file = e.dataTransfer.files[0];
      if (file) onFileSelected(file);
    },
    [onFileSelected]
  );

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) onFileSelected(file);
  };

  const fmt = (bytes: number) => {
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / (1024 * 1024)).toFixed(1) + " MB";
  };

  return (
    <div className="flex flex-col h-full p-6">
      <div
        className={`flex-1 flex flex-col items-center justify-center border-2 border-dashed rounded-2xl transition-all cursor-pointer ${
          isDragging
            ? "border-blue-400 bg-blue-500/8"
            : selectedFile
            ? "border-blue-500/40 bg-blue-500/5"
            : "border-white/15 hover:border-white/30 hover:bg-white/3"
        }`}
        onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        onClick={() => inputRef.current?.click()}
      >
        <input
          ref={inputRef}
          type="file"
          className="hidden"
          accept={ACCEPTED_MIME[mode]}
          onChange={handleChange}
        />

        {selectedFile ? (
          <div className="text-center space-y-3 px-6">
            <div className="w-14 h-14 mx-auto rounded-xl bg-blue-500/15 flex items-center justify-center">
              <span className="text-2xl">{mode === "general" ? "🎬" : "📷"}</span>
            </div>
            <div>
              <p className="text-sm font-medium text-white/90 truncate max-w-[240px]">
                {selectedFile.name}
              </p>
              <p className="text-xs text-white/40 mt-1">{fmt(selectedFile.size)}</p>
            </div>
            <button
              className="text-xs text-blue-400 hover:text-blue-300 underline underline-offset-2"
              onClick={(e) => { e.stopPropagation(); inputRef.current?.click(); }}
            >
              파일 변경
            </button>
          </div>
        ) : (
          <div className="text-center space-y-4 px-6">
            <div className="w-16 h-16 mx-auto rounded-2xl bg-white/5 flex items-center justify-center">
              <svg className="w-7 h-7 text-white/30" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                  d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
              </svg>
            </div>
            <div>
              <p className="text-sm font-medium text-white/70">
                파일을 드래그하거나 클릭하여 업로드
              </p>
              <p className="text-xs text-white/35 mt-1.5">
                지원 형식: {ACCEPTED[mode].join(", ").toUpperCase()}
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
