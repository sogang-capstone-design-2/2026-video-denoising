"use client";

import { useState } from "react";

export default function LowLightOptions() {
  const [showInfo, setShowInfo] = useState(false);

  return (
    <div className="flex flex-col gap-6">
      {/* Warning banner */}
      <div className="flex gap-3 px-4 py-3 rounded-xl bg-amber-500/10 border border-amber-500/25">
        <span className="text-amber-400 shrink-0 mt-0.5">⚠</span>
        <div>
          <p className="text-sm font-medium text-amber-300">RAW 파일만 지원됩니다</p>
          <p className="text-xs text-amber-400/70 mt-1">
            저조도 디노이징은 RAW 센서 데이터를 활용합니다.
          </p>
        </div>
      </div>

      {/* Supported formats */}
      <div>
        <p className="text-xs font-semibold uppercase tracking-widest text-white/30 mb-3">
          지원 형식
        </p>
        <div className="flex flex-wrap gap-2">
          {["RAW", "DNG", "NEF", "CR2", "ARW"].map((fmt) => (
            <span
              key={fmt}
              className="px-2.5 py-1 text-xs font-mono rounded-md bg-white/6 text-white/50 border border-white/8"
            >
              {fmt}
            </span>
          ))}
        </div>
      </div>

      {/* RAW info toggle */}
      <div>
        <button
          onClick={() => setShowInfo((v) => !v)}
          className="flex items-center gap-2 text-xs text-blue-400 hover:text-blue-300 transition-colors"
        >
          <span className="w-4 h-4 rounded-full border border-blue-400/60 flex items-center justify-center text-[10px]">
            i
          </span>
          RAW 파일이란?
        </button>

        {showInfo && (
          <div className="mt-3 px-4 py-3 rounded-xl bg-white/4 border border-white/8 text-xs text-white/50 leading-relaxed space-y-1.5">
            <p>
              RAW 파일은 카메라 이미지 센서가 캡처한 원본 데이터로, JPEG처럼
              손실 압축이 적용되지 않은 형식입니다.
            </p>
            <p>
              최대 비트 심도(12–14bit)의 노이즈 정보가 보존되어 AI 디노이징
              성능이 크게 향상됩니다.
            </p>
            <p className="text-white/35">
              카메라 제조사별로 확장자가 다릅니다: Canon(CR2/CR3), Nikon(NEF),
              Sony(ARW), Adobe(DNG) 등.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
