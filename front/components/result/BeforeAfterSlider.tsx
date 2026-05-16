"use client";

import { useState, useRef, useCallback } from "react";

interface BeforeAfterSliderProps {
  beforeUrl: string;
  afterUrl: string;
  beforeLabel?: string;
  afterLabel?: string;
}

export default function BeforeAfterSlider({
  beforeUrl,
  afterUrl,
  beforeLabel = "Before",
  afterLabel = "After",
}: BeforeAfterSliderProps) {
  const [position, setPosition] = useState(50); // percent
  const containerRef = useRef<HTMLDivElement>(null);
  const dragging = useRef(false);

  const updatePosition = useCallback((clientX: number) => {
    const container = containerRef.current;
    if (!container) return;
    const rect = container.getBoundingClientRect();
    const pct = Math.max(0, Math.min(100, ((clientX - rect.left) / rect.width) * 100));
    setPosition(pct);
  }, []);

  const onMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    dragging.current = true;

    const onMove = (e: MouseEvent) => {
      if (dragging.current) updatePosition(e.clientX);
    };
    const onUp = () => {
      dragging.current = false;
      window.removeEventListener("mousemove", onMove);
      window.removeEventListener("mouseup", onUp);
    };
    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseup", onUp);
  };

  const onTouchStart = (e: React.TouchEvent) => {
    const onMove = (e: TouchEvent) => {
      updatePosition(e.touches[0].clientX);
    };
    const onEnd = () => {
      window.removeEventListener("touchmove", onMove);
      window.removeEventListener("touchend", onEnd);
    };
    window.addEventListener("touchmove", onMove);
    window.addEventListener("touchend", onEnd);
  };

  return (
    <div
      ref={containerRef}
      className="relative overflow-hidden rounded-2xl select-none cursor-ew-resize bg-black"
      style={{ aspectRatio: "16/9" }}
      onClick={(e) => updatePosition(e.clientX)}
    >
      {/* Before image (base layer) */}
      <img
        src={beforeUrl}
        alt="Before"
        className="absolute inset-0 w-full h-full object-cover"
        draggable={false}
      />

      {/* After image (clipped to right portion) */}
      <div
        className="absolute inset-0 overflow-hidden"
        style={{ clipPath: `polygon(${position}% 0, 100% 0, 100% 100%, ${position}% 100%)` }}
      >
        <img
          src={afterUrl}
          alt="After"
          className="absolute inset-0 w-full h-full object-cover"
          draggable={false}
        />
      </div>

      {/* Divider line */}
      <div
        className="absolute top-0 bottom-0 w-px bg-white/80 shadow-[0_0_8px_rgba(255,255,255,0.6)]"
        style={{ left: `${position}%` }}
      />

      {/* Drag handle */}
      <div
        className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 z-10 flex items-center justify-center"
        style={{ left: `${position}%` }}
        onMouseDown={onMouseDown}
        onTouchStart={onTouchStart}
      >
        <div className="w-9 h-9 rounded-full bg-white shadow-lg flex items-center justify-center border border-white/30">
          <svg className="w-4 h-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5}
              d="M8 9l-4 3 4 3M16 9l4 3-4 3" />
          </svg>
        </div>
      </div>

      {/* Labels */}
      <div className="absolute bottom-4 left-4 px-2.5 py-1 rounded-md bg-black/60 backdrop-blur-sm text-xs font-semibold text-white/80">
        {beforeLabel}
      </div>
      <div className="absolute bottom-4 right-4 px-2.5 py-1 rounded-md bg-blue-500/80 backdrop-blur-sm text-xs font-semibold text-white">
        {afterLabel}
      </div>
    </div>
  );
}
