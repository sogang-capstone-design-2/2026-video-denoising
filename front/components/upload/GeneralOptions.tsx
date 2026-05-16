"use client";

import type { ProcessingIntensity } from "@/types";

interface GeneralOptionsProps {
  intensity: ProcessingIntensity;
  onChange: (v: ProcessingIntensity) => void;
}

const options: { value: ProcessingIntensity; label: string; desc: string }[] = [
  { value: "low", label: "낮음", desc: "빠른 처리, 가벼운 노이즈 제거" },
  { value: "medium", label: "보통", desc: "균형잡힌 품질과 속도" },
  { value: "high", label: "높음", desc: "최고 품질, 느린 처리" },
];

export default function GeneralOptions({ intensity, onChange }: GeneralOptionsProps) {
  return (
    <div className="flex flex-col gap-6">
      {/* Processing intensity */}
      <div>
        <p className="text-xs font-semibold uppercase tracking-widest text-white/30 mb-3">
          처리 강도
        </p>
        <div className="space-y-2">
          {options.map((opt) => (
            <button
              key={opt.value}
              onClick={() => onChange(opt.value)}
              className={`w-full text-left px-4 py-3 rounded-xl border transition-all ${
                intensity === opt.value
                  ? "border-blue-500/50 bg-blue-500/10 text-white"
                  : "border-white/8 bg-white/3 text-white/60 hover:bg-white/6 hover:text-white/80"
              }`}
            >
              <div className="flex items-center gap-3">
                <span
                  className={`w-3.5 h-3.5 rounded-full border-2 shrink-0 ${
                    intensity === opt.value
                      ? "border-blue-400 bg-blue-400"
                      : "border-white/25"
                  }`}
                />
                <div>
                  <p className="text-sm font-medium leading-none">{opt.label}</p>
                  <p className="text-[11px] mt-1 text-white/40">{opt.desc}</p>
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Supported formats */}
      <div>
        <p className="text-xs font-semibold uppercase tracking-widest text-white/30 mb-3">
          지원 형식
        </p>
        <div className="flex flex-wrap gap-2">
          {["MP4", "MOV", "AVI"].map((fmt) => (
            <span
              key={fmt}
              className="px-2.5 py-1 text-xs font-mono rounded-md bg-white/6 text-white/50 border border-white/8"
            >
              {fmt}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}
