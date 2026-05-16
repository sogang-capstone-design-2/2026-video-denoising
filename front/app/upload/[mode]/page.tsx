"use client";

import { useState, use } from "react";
import { useRouter } from "next/navigation";
import Sidebar from "@/components/layout/Sidebar";
import DropZone from "@/components/upload/DropZone";
import GeneralOptions from "@/components/upload/GeneralOptions";
import LowLightOptions from "@/components/upload/LowLightOptions";
import { saveUploadSession } from "@/lib/session";
import type { Mode, ProcessingIntensity } from "@/types";

interface Props {
  params: Promise<{ mode: string }>;
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp", "image/tiff", "image/gif"];

export default function UploadPage({ params }: Props) {
  const { mode: rawMode } = use(params);
  const mode: Mode = rawMode === "lowlight" ? "lowlight" : "general";

  const router = useRouter();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [intensity, setIntensity] = useState<ProcessingIntensity>("medium");
  const [isStarting, setIsStarting] = useState(false);

  const handleStart = async () => {
    if (!selectedFile) return;
    setIsStarting(true);

    let previewUrl: string | null = null;
    if (IMAGE_TYPES.includes(selectedFile.type)) {
      previewUrl = await readFileAsDataUrl(selectedFile);
    }

    saveUploadSession({
      mode,
      intensity,
      fileName: selectedFile.name,
      fileSize: selectedFile.size,
      previewUrl,
    });

    router.push("/processing");
  };

  return (
    <div className="flex h-screen bg-[#08080c] overflow-hidden">
      <Sidebar activeMode={mode} />

      {/* Main content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <header className="px-6 py-4 border-b border-white/6 flex items-center gap-3">
          <h1 className="text-sm font-semibold text-white/80">
            {mode === "general" ? "일반 영상 화질 개선" : "저조도 영상 화질 개선"}
          </h1>
          <span className="text-[11px] px-2 py-0.5 rounded-md bg-white/6 text-white/40 border border-white/8">
            {mode === "general" ? "RGB · FastDVDnet" : "RAW · RViDeNet"}
          </span>
        </header>

        {/* Body: options + drop zone + history */}
        <div className="flex-1 flex overflow-hidden">
          {/* Options panel */}
          <div className="w-64 shrink-0 border-r border-white/6 px-5 py-6 overflow-y-auto">
            {mode === "general" ? (
              <GeneralOptions intensity={intensity} onChange={setIntensity} />
            ) : (
              <LowLightOptions />
            )}
          </div>

          {/* Drop zone */}
          <div className="flex-1 overflow-hidden">
            <DropZone
              mode={mode}
              selectedFile={selectedFile}
              onFileSelected={setSelectedFile}
            />
          </div>

          {/* Recent history panel */}
          <div className="w-52 shrink-0 border-l border-white/6 px-4 py-6 overflow-y-auto">
            <p className="text-[11px] font-semibold uppercase tracking-widest text-white/25 mb-4">
              최근 작업
            </p>
            <div className="flex flex-col gap-2">
              {/* Placeholder history items */}
              {["example_raw.dng", "night_shot.mp4", "lowlight_01.raw"].map((name, i) => (
                <div
                  key={i}
                  className="px-3 py-2.5 rounded-lg bg-white/3 border border-white/6"
                >
                  <p className="text-xs text-white/50 truncate">{name}</p>
                  <p className="text-[10px] text-white/25 mt-0.5">
                    {i === 0 ? "방금 전" : i === 1 ? "2시간 전" : "어제"}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Footer: Start button */}
        <div className="border-t border-white/6 px-6 py-4 flex items-center justify-between bg-[#0a0a10]">
          <p className="text-xs text-white/35">
            {selectedFile
              ? `선택됨: ${selectedFile.name}`
              : "파일을 선택하면 처리를 시작할 수 있습니다."}
          </p>
          <button
            onClick={handleStart}
            disabled={!selectedFile || isStarting}
            className="flex items-center gap-2 px-6 py-2.5 rounded-full bg-blue-500 hover:bg-blue-400 disabled:opacity-30 disabled:cursor-not-allowed text-white text-sm font-semibold transition-all hover:scale-105 disabled:hover:scale-100 shadow-lg shadow-blue-500/20"
          >
            {isStarting ? (
              <>
                <span className="w-3.5 h-3.5 rounded-full border-2 border-white/30 border-t-white animate-spin" />
                준비 중...
              </>
            ) : (
              "처리 시작 →"
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
