"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import ModeCard from "@/components/common/ModeCard";

export default function SelectPage() {
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
      </nav>

      {/* Content */}
      <div className="flex-1 flex flex-col items-center justify-center px-6 py-16">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="w-full max-w-2xl"
        >
          {/* Heading */}
          <div className="text-center mb-10">
            <h2 className="text-2xl sm:text-3xl font-bold text-white mb-3">
              어떤 영상을 개선하시겠어요?
            </h2>
            <p className="text-sm text-white/40">
              모드를 선택하면 해당 업로드 화면으로 이동합니다.
            </p>
          </div>

          {/* Mode cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.45, delay: 0.1 }}
            >
              <ModeCard mode="general" />
            </motion.div>
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.45, delay: 0.2 }}
            >
              <ModeCard mode="lowlight" />
            </motion.div>
          </div>
        </motion.div>
      </div>
    </main>
  );
}
