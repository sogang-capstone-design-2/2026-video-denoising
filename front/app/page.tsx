"use client";

import Link from "next/link";
import { motion } from "framer-motion";

const features = [
  {
    icon: "📷",
    title: "RAW 기반 저조도 디노이징",
    desc: "카메라 센서 원본 데이터를 활용해 어두운 환경의 영상을 정밀하게 복원합니다.",
  },
  {
    icon: "🎬",
    title: "RGB 범용 디노이징",
    desc: "일반 MP4·MOV 영상의 노이즈를 AI 모델로 빠르게 제거합니다.",
  },
  {
    icon: "⚡",
    title: "Before / After 비교",
    desc: "처리 전·후 결과를 인터랙티브 슬라이더로 즉시 확인하고 다운로드하세요.",
  },
];

const fadeUp = {
  hidden: { opacity: 0, y: 24 },
  visible: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { delay: i * 0.12, duration: 0.55, ease: "easeOut" as const },
  }),
};

export default function LandingPage() {
  return (
    <main className="min-h-screen bg-[#08080c] flex flex-col">
      {/* Nav */}
      <nav className="flex items-center justify-between px-8 py-5 border-b border-white/6">
        <div className="flex items-center gap-2">
          <span className="text-blue-400 text-xl">◈</span>
          <span className="text-sm font-semibold text-white/80">AI Driven Night Vision</span>
        </div>
        <Link
          href="/select"
          className="text-sm text-white/50 hover:text-white/90 transition-colors"
        >
          시작하기 →
        </Link>
      </nav>

      {/* Hero */}
      <section className="flex-1 flex flex-col items-center justify-center text-center px-6 py-24">
        {/* Badge */}
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4 }}
          className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-500/12 border border-blue-500/25 text-xs text-blue-300 font-medium mb-8"
        >
          <span className="w-1.5 h-1.5 rounded-full bg-blue-400 animate-pulse" />
          AI-Powered Denoising Demo
        </motion.div>

        {/* Title */}
        <motion.h1
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.1 }}
          className="text-5xl sm:text-6xl font-extrabold tracking-tight text-white mb-5 leading-tight"
        >
          AI Driven
          <br />
          <span className="bg-gradient-to-r from-blue-400 to-violet-400 bg-clip-text text-transparent">
            Night Vision
          </span>
        </motion.h1>

        {/* Subtitle */}
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="text-base sm:text-lg text-white/50 max-w-md leading-relaxed mb-10"
        >
          저조도 환경 영상의 노이즈를 AI로 제거하여
          <br />
          선명한 시인성을 확보합니다.
        </motion.p>

        {/* CTA */}
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.35 }}
        >
          <Link
            href="/select"
            className="inline-flex items-center gap-2 px-7 py-3.5 rounded-full bg-blue-500 hover:bg-blue-400 text-white font-semibold text-sm transition-all shadow-lg shadow-blue-500/25 hover:shadow-blue-400/35 hover:scale-105"
          >
            지금 바로 시작하기
            <span>→</span>
          </Link>
        </motion.div>
      </section>

      {/* Feature cards */}
      <section className="px-8 pb-20 max-w-4xl mx-auto w-full">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
          {features.map((f, i) => (
            <motion.div
              key={f.title}
              custom={i}
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, margin: "-40px" }}
              variants={fadeUp}
              className="p-6 rounded-2xl bg-[#111118] border border-white/6 hover:border-white/12 transition-colors"
            >
              <div className="text-2xl mb-3">{f.icon}</div>
              <h3 className="text-sm font-semibold text-white/90 mb-2">{f.title}</h3>
              <p className="text-xs text-white/45 leading-relaxed">{f.desc}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-white/6 px-8 py-5 text-center text-xs text-white/25">
        Sogang University · Capstone Design 2 · 멋진돌 팀
      </footer>
    </main>
  );
}
