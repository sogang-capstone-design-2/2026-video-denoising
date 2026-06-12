import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // 요청 본문 크기 제한 증가 (기본값 1MB → 2GB)
  api: {
    bodyParser: {
      sizeLimit: "2gb",
    },
    responseLimit: "2gb",
  },
  async rewrites() {
    return {
      beforeFiles: [
        {
          source: "/api/:path*",
          destination: "http://localhost:8080/api/:path*",
        },
      ],
    };
  },
};

export default nextConfig;
