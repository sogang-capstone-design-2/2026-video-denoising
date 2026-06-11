"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { loadUploadSession, saveResultSession } from "@/lib/session";
import { getFile, clearFile } from "@/lib/fileStore";
import { submitJob, pollUntilDone, getJobStatus } from "@/lib/api";
import type { JobPhase } from "@/lib/api";
import type { UploadSession } from "@/types";

type UiPhase = "uploading" | "processing" | "finalizing";

const PHASE_LABELS: Record<UiPhase, string> = {
  uploading: "파일 업로드 중...",
  processing: "AI 처리 중...",
  finalizing: "결과 준비 중...",
};

const STEPS = [
  { key: "uploading", label: "업로드" },
  { key: "processing", label: "AI 처리" },
  { key: "finalizing", label: "완료" },
] as const;

function toUiPhase(phase: JobPhase): UiPhase {
  if (phase === "processing") return "processing";
  if (phase === "finalizing" || phase === "done") return "finalizing";
  return "uploading";
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

export default function ProcessingPage() {
  const router = useRouter();
  const [progress, setProgress] = useState(0);
  const [phase, setPhase] = useState<UiPhase>("uploading");
  const [session, setSession] = useState<UploadSession | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const s = loadUploadSession();
    const file = getFile();

    if (!s || !file) {
      router.replace("/select");
      return;
    }
    setSession(s);

    (async () => {
      try {
        setPhase("uploading");
        setProgress(10);

        const jobId = await submitJob(file, s.mode, s.intensity);
        clearFile();

        setProgress(30);

        if (s.mode === "lowlight") {
          // lowlight: noisy PNG가 준비되는 즉시 result 페이지로 이동
          let noisyNavigated = false;
          while (true) {
            const status = await getJobStatus(jobId);
            setPhase(toUiPhase(status.phase));
            setProgress(status.percent);

            if (status.phase === "failed") {
              throw new Error(status.error ?? "처리 실패");
            }

            // noisy 준비됨 → 즉시 이동 (denoised는 result 페이지에서 대기)
            if (status.noisyImageUrl && !noisyNavigated) {
              noisyNavigated = true;
              saveResultSession({
                jobId,
                mode: s.mode,
                fileName: s.fileName,
                beforeUrl: `/api/jobs/${jobId}/files/before`,
                afterUrl: null, // denoised 아직 처리 중
              });
              router.push("/result");
              return;
            }

            // fallback: noisyImageUrl 없이 바로 완료된 경우
            if (status.phase === "done") {
              saveResultSession({
                jobId,
                mode: s.mode,
                fileName: s.fileName,
                beforeUrl: `/api/jobs/${jobId}/files/before`,
                afterUrl: `/api/jobs/${jobId}/files/after`,
              });
              router.push("/result");
              return;
            }

            await sleep(1500);
          }
        } else {
          // general: 완료될 때까지 폴링 후 이동
          await pollUntilDone(jobId, (status) => {
            setPhase(toUiPhase(status.phase));
            setProgress(status.percent);
          });

          saveResultSession({
            jobId,
            mode: s.mode,
            fileName: s.fileName,
            beforeUrl: `/api/jobs/${jobId}/files/before`,
            afterUrl: `/api/jobs/${jobId}/files/after`,
          });
          router.push("/result");
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다.");
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const currentStepIndex = STEPS.findIndex((s) => s.key === phase);

  if (error) {
    return (
      <main className="min-h-screen bg-[#08080c] flex flex-col items-center justify-center px-6">
        <div className="w-full max-w-sm text-center">
          <div className="w-16 h-16 mx-auto mb-6 rounded-2xl bg-red-500/15 border border-red-500/25 flex items-center justify-center text-2xl">
            ⚠️
          </div>
          <p className="text-sm text-red-400 mb-2">처리 중 오류가 발생했습니다</p>
          <p className="text-xs text-white/30 mb-8">{error}</p>
          <button
            onClick={() => router.back()}
            className="px-6 py-2.5 rounded-full bg-white/6 hover:bg-white/10 border border-white/10 text-white/70 text-sm font-semibold transition-all"
          >
            돌아가기
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#08080c] flex flex-col items-center justify-center px-6">
      <div className="w-full max-w-sm">
        {/* Icon */}
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          className="w-16 h-16 mx-auto mb-8 rounded-2xl bg-blue-500/15 border border-blue-500/25 flex items-center justify-center"
        >
          <span className="text-2xl">
            {phase === "uploading" ? "📤" : phase === "processing" ? "🤖" : "✨"}
          </span>
        </motion.div>

        {/* Phase label */}
        <motion.p
          key={phase}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center text-sm font-medium text-white/70 mb-2"
        >
          {PHASE_LABELS[phase]}
        </motion.p>

        {/* File name */}
        {session && (
          <p className="text-center text-xs text-white/30 mb-6 truncate px-4">
            {session.fileName}
          </p>
        )}

        {/* Progress bar */}
        <div className="relative h-1.5 bg-white/8 rounded-full overflow-hidden mb-4">
          <motion.div
            className="absolute inset-y-0 left-0 bg-gradient-to-r from-blue-500 to-violet-500 rounded-full"
            style={{ width: `${progress}%` }}
            transition={{ ease: "easeOut" as const }}
          />
        </div>

        <p className="text-center text-xs text-white/35 mb-8">{progress}%</p>

        {/* Steps */}
        <div className="flex items-center justify-center gap-0">
          {STEPS.map((step, i) => {
            const done = i < currentStepIndex;
            const active = i === currentStepIndex;
            return (
              <div key={step.key} className="flex items-center">
                {i > 0 && (
                  <div
                    className={`h-px w-10 transition-colors duration-500 ${
                      i <= currentStepIndex ? "bg-blue-500" : "bg-white/12"
                    }`}
                  />
                )}
                <div className="flex flex-col items-center gap-1.5">
                  <div
                    className={`w-2.5 h-2.5 rounded-full border transition-all duration-300 ${
                      done
                        ? "bg-blue-500 border-blue-500"
                        : active
                        ? "bg-blue-400 border-blue-400 shadow-[0_0_6px_rgba(96,165,250,0.6)]"
                        : "bg-transparent border-white/20"
                    }`}
                  />
                  <span className={`text-[10px] ${active ? "text-blue-300" : "text-white/25"}`}>
                    {step.label}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </main>
  );
}
