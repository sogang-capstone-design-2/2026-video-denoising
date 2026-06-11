"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { motion } from "framer-motion";
import { loadResultSession, clearSessions } from "@/lib/session";
import { getJobStatus } from "@/lib/api";
import type { ResultSession } from "@/types";

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

export default function ResultPage() {
  const router = useRouter();
  const [result, setResult] = useState<ResultSession | null>(null);
  const [afterUrl, setAfterUrl] = useState<string | null>(null);
  const [isDenoising, setIsDenoising] = useState(false);
  const [denoisedError, setDenoisedError] = useState<string | null>(null);

  // 세션 로드
  useEffect(() => {
    const r = loadResultSession();
    if (!r) {
      router.replace("/select");
      return;
    }
    setResult(r);
    setAfterUrl(r.afterUrl);
    setIsDenoising(r.afterUrl === null);
  }, [router]);

  // lowlight: afterUrl이 null이면 denoised 완료까지 폴링
  useEffect(() => {
    if (!result || !isDenoising) return;

    let cancelled = false;
    (async () => {
      try {
        while (!cancelled) {
          const status = await getJobStatus(result.jobId);

          if (status.phase === "failed") {
            setDenoisedError(status.error ?? "디노이즈 처리 실패");
            setIsDenoising(false);
            return;
          }
          if (status.phase === "done") {
            const url = `/api/jobs/${result.jobId}/files/after`;
            setAfterUrl(url);
            setIsDenoising(false);
            return;
          }
          await sleep(2000);
        }
      } catch {
        if (!cancelled) setDenoisedError("상태 조회 중 오류가 발생했습니다.");
      }
    })();

    return () => { cancelled = true; };
  }, [result, isDenoising]);

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
          ✓ {isDenoising ? "입력 이미지 준비 완료 — 디노이즈 처리 중..." : "처리가 완료되었습니다"}
        </motion.div>

        <p className="text-sm text-white/40 mb-8">{result.fileName}</p>

        {/* Before / After 미디어 */}
        <motion.div
          initial={{ opacity: 0, scale: 0.98 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5 }}
          className="w-full mb-8 grid grid-cols-2 gap-4"
        >
          {/* Before */}
          <div className="flex flex-col gap-2">
            <p className="text-[11px] font-semibold uppercase tracking-widest text-white/30 text-center">
              원본 (Noisy)
            </p>
            <div className="rounded-2xl overflow-hidden bg-[#111118] border border-white/8">
              {result.mode === "lowlight" ? (
                <img
                  src={result.beforeUrl}
                  alt="원본"
                  className="w-full aspect-video object-contain bg-black"
                />
              ) : (
                <video
                  src={result.beforeUrl}
                  controls autoPlay loop muted playsInline
                  className="w-full aspect-video object-contain bg-black"
                />
              )}
            </div>
          </div>

          {/* After */}
          <div className="flex flex-col gap-2">
            <p className="text-[11px] font-semibold uppercase tracking-widest text-white/30 text-center">
              처리 후 (Denoised)
            </p>
            <div className="rounded-2xl overflow-hidden bg-[#111118] border border-white/8">
              {denoisedError ? (
                <div className="flex flex-col items-center justify-center aspect-video text-center px-4">
                  <span className="text-2xl mb-2">⚠️</span>
                  <p className="text-xs text-red-400">{denoisedError}</p>
                </div>
              ) : isDenoising || !afterUrl ? (
                <div className="flex flex-col items-center justify-center aspect-video gap-3">
                  <div className="w-8 h-8 rounded-full border-2 border-white/20 border-t-blue-400 animate-spin" />
                  <p className="text-xs text-white/30">RViDeNet 추론 중...</p>
                </div>
              ) : result.mode === "lowlight" ? (
                <img
                  src={afterUrl}
                  alt="디노이즈 결과"
                  className="w-full aspect-video object-contain bg-black"
                />
              ) : (
                <video
                  src={afterUrl}
                  controls autoPlay loop muted playsInline
                  className="w-full aspect-video object-contain bg-black"
                />
              )}
            </div>
          </div>
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
          {afterUrl && !isDenoising && (
            <a
              href={afterUrl}
              download={
                result.mode === "lowlight"
                  ? `denoised_${result.fileName.replace(/\.[^.]+$/, "")}.png`
                  : `denoised_${result.fileName}`
              }
              className="flex items-center gap-2 px-6 py-2.5 rounded-full bg-blue-500 hover:bg-blue-400 text-white text-sm font-semibold transition-all hover:scale-105 shadow-lg shadow-blue-500/20"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
              </svg>
              결과 다운로드
            </a>
          )}
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
