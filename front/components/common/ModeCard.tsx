"use client";

import { useRouter } from "next/navigation";
import type { Mode } from "@/types";

interface ModeCardProps {
  mode: Mode;
}

const config: Record<
  Mode,
  { icon: string; title: string; subtitle: string; desc: string; formats: string[]; color: string }
> = {
  general: {
    icon: "🎬",
    title: "일반 영상 화질 개선",
    subtitle: "RGB 기반 범용 디노이징",
    desc: "MP4, MOV 영상 파일의 노이즈를 AI로 제거하여 선명한 화질로 개선합니다.",
    formats: ["MP4", "MOV", "AVI"],
    color: "from-blue-600/20 to-indigo-600/10 border-blue-500/25 hover:border-blue-400/50",
  },
  lowlight: {
    icon: "🌙",
    title: "저조도 영상 화질 개선",
    subtitle: "RAW 기반 저조도 특화 디노이징",
    desc: "RAW 센서 데이터를 활용해 어두운 환경의 영상에서 노이즈를 정밀하게 제거합니다.",
    formats: ["RAW", "DNG", "NEF", "CR2"],
    color: "from-violet-600/20 to-purple-600/10 border-violet-500/25 hover:border-violet-400/50",
  },
};

export default function ModeCard({ mode }: ModeCardProps) {
  const router = useRouter();
  const c = config[mode];

  return (
    <button
      onClick={() => router.push(`/upload/${mode}`)}
      className={`group relative w-full text-left bg-gradient-to-br ${c.color} border rounded-2xl p-7 transition-all duration-200 hover:scale-[1.02] hover:shadow-2xl`}
    >
      {/* Icon */}
      <div className="text-4xl mb-5">{c.icon}</div>

      {/* Text */}
      <h3 className="text-lg font-bold text-white mb-1">{c.title}</h3>
      <p className="text-xs font-medium text-white/40 mb-3 uppercase tracking-widest">
        {c.subtitle}
      </p>
      <p className="text-sm text-white/60 leading-relaxed mb-5">{c.desc}</p>

      {/* Formats */}
      <div className="flex flex-wrap gap-2">
        {c.formats.map((fmt) => (
          <span
            key={fmt}
            className="px-2 py-0.5 text-[11px] font-mono rounded bg-white/8 text-white/50 border border-white/10"
          >
            {fmt}
          </span>
        ))}
      </div>

      {/* Arrow */}
      <div className="absolute top-6 right-6 w-8 h-8 rounded-full bg-white/6 border border-white/10 flex items-center justify-center group-hover:bg-white/12 transition-colors">
        <span className="text-white/50 text-sm group-hover:text-white/80 transition-colors">→</span>
      </div>
    </button>
  );
}
