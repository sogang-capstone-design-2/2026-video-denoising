"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { motion } from "framer-motion";
import { loadResultSession, clearSessions } from "@/lib/session";
import type { ResultSession } from "@/types";

export default function ResultPage() {
  const router = useRouter();
  const [result, setResult] = useState<ResultSession | null>(null);

  useEffect(() => {
    const r = loadResultSession();
    if (!r) {
      router.replace("/select");
      return;
    }
    setResult(r);
  }, [router]);

  const handleRestart = () => {
    clearSessions();
    router.push("/select");
  };

  if (!result) return null;

  return (
    <main className="min-h-screen bg-[#08080c] flex flex-col">
      {/* Nav */}
      <nav className="flex items-center justify-between px-8 py-5 border-b border-white/6">
        <Link href="/" className="flex items-center gap-2 group">
          <span className="text-blue-400 text-xl">◈</span>
          <span className="text-sm font-semibold text-white/80 group-hover:text-white transition-colors">
            AI Driven Night Vision
          </span>
        </Link>
        <div className="flex items-center gap-3">
          <span className="text-xs text-white/30">
            {result.mode === "general" ? "일반 영상" : "저조도 영상"} 처리 완료
          </span>
          <span className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
        </div>
      </nav>

      <div className="flex-1 flex flex-col items-center justify-start px-6 py-10 max-w-5xl mx-auto w-full">
        {/* Success badge */}
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-green-500/12 border border-green-500/25 text-xs text-green-300 font-medium mb-6"
        >
          ✓ 처리가 완료되었습니다
        </motion.div>

        <p className="text-sm text-white/40 mb-8">{result.fileName}</p>

        {/* Before / After video players */}
        <motion.div
          initial={{ opacity: 0, scale: 0.98 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5 }}
          className="w-full mb-8 grid grid-cols-2 gap-4"
        >
          {(["before", "after"] as const).map((side) => (
            <div key={side} className="flex flex-col gap-2">
              <p className="text-[11px] font-semibold uppercase tracking-widest text-white/30 text-center">
                {side === "before" ? "원본 (Noisy)" : "처리 후 (Denoised)"}
              </p>
              <div className="rounded-2xl overflow-hidden bg-[#111118] border border-white/8">
                <video
                  src={side === "before" ? result.beforeUrl : result.afterUrl}
                  controls
                  autoPlay
                  loop
                  muted
                  playsInline
                  className="w-full aspect-video object-contain bg-black"
                />
              </div>
            </div>
          ))}
        </motion.div>

        {/* Stats row */}
        <div className="flex gap-4 mb-8 w-full">
          {[
            { label: "처리 모드", value: result.mode === "general" ? "RGB 범용" : "RAW 저조도" },
            { label: "모델", value: result.mode === "general" ? "FastDVDnet" : "RViDeNet" },
            { label: "파일", value: result.fileName },
          ].map((s) => (
            <div
              key={s.label}
              className="flex-1 px-4 py-3 rounded-xl bg-[#111118] border border-white/6"
            >
              <p className="text-[10px] uppercase tracking-widest text-white/25 mb-1">{s.label}</p>
              <p className="text-sm font-medium text-white/70 truncate">{s.value}</p>
            </div>
          ))}
        </div>

        {/* Action buttons */}
        <div className="flex gap-3">
          <a
            href={result.afterUrl}
            download={`denoised_${result.fileName}`}
            className="flex items-center gap-2 px-6 py-2.5 rounded-full bg-blue-500 hover:bg-blue-400 text-white text-sm font-semibold transition-all hover:scale-105 shadow-lg shadow-blue-500/20"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            결과 다운로드
          </a>
          <button
            onClick={handleRestart}
            className="flex items-center gap-2 px-6 py-2.5 rounded-full bg-white/6 hover:bg-white/10 border border-white/10 text-white/70 hover:text-white text-sm font-semibold transition-all"
          >
            다시 시작
          </button>
        </div>
      </div>
    </main>
  );
}
