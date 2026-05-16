"use client";

import Link from "next/link";
import type { Mode } from "@/types";

interface SidebarProps {
  activeMode: Mode;
}

const modes: { id: Mode; label: string; sub: string }[] = [
  { id: "general", label: "일반 영상", sub: "MP4 · MOV · AVI" },
  { id: "lowlight", label: "저조도 영상", sub: "RAW · DNG" },
];

export default function Sidebar({ activeMode }: SidebarProps) {
  return (
    <aside className="w-52 shrink-0 bg-[#0f0f16] border-r border-white/8 flex flex-col">
      {/* Logo */}
      <div className="px-5 py-5 border-b border-white/8">
        <Link href="/" className="flex items-center gap-2 group">
          <span className="text-blue-400 text-lg">◈</span>
          <span className="text-sm font-semibold text-white/90 group-hover:text-white transition-colors">
            Night Vision
          </span>
        </Link>
      </div>

      {/* Mode list */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        <p className="px-2 pb-2 text-[11px] font-medium uppercase tracking-widest text-white/30">
          모드
        </p>
        {modes.map((m) => {
          const isActive = m.id === activeMode;
          return (
            <Link
              key={m.id}
              href={`/upload/${m.id}`}
              className={`flex flex-col px-3 py-2.5 rounded-lg transition-all ${
                isActive
                  ? "bg-blue-500/15 text-blue-300 border border-blue-500/25"
                  : "text-white/60 hover:bg-white/5 hover:text-white/90 border border-transparent"
              }`}
            >
              <span className="text-sm font-medium">{m.label}</span>
              <span className="text-[11px] mt-0.5 opacity-60">{m.sub}</span>
            </Link>
          );
        })}
      </nav>

      {/* Bottom: back to home */}
      <div className="px-3 pb-5 border-t border-white/8 pt-4">
        <Link
          href="/select"
          className="flex items-center gap-2 px-3 py-2 text-xs text-white/40 hover:text-white/70 transition-colors rounded-lg hover:bg-white/5"
        >
          ← 모드 선택으로
        </Link>
      </div>
    </aside>
  );
}
