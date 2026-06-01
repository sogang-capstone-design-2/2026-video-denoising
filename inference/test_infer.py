"""
P2.1 — 단일 이미지 추론 CLI 테스트
사용법: python3 test_infer.py <input_path> <mode> [intensity]
예시:  python3 test_infer.py /tmp/test.jpg general medium
"""

import sys
import os
from models.fastdvdnet import FastDVDnet
from models.rvdenet import RViDeNet


def main():
    if len(sys.argv) < 3:
        print("사용법: python3 test_infer.py <input_path> <mode> [intensity]")
        print("  mode: general | lowlight")
        print("  intensity: low | medium | high (기본값: medium)")
        sys.exit(1)

    input_path = sys.argv[1]
    mode = sys.argv[2]
    intensity = sys.argv[3] if len(sys.argv) > 3 else "medium"

    # 입력 파일 존재 확인
    if not os.path.exists(input_path):
        print(f"[오류] 파일을 찾을 수 없습니다: {input_path}")
        sys.exit(1)

    # 출력 경로 생성
    base, ext = os.path.splitext(input_path)
    output_path = f"{base}_denoised{ext}"

    # 모델 로드
    print(f"모드: {mode} / intensity: {intensity}")
    if mode == "general":
        model = FastDVDnet()
    elif mode == "lowlight":
        model = RViDeNet()
    else:
        print(f"[오류] 지원하지 않는 모드: {mode}")
        sys.exit(1)

    # 추론 실행
    print(f"추론 시작: {input_path}")
    model.infer(input_path, output_path)
    print(f"추론 완료: {output_path}")


if __name__ == "__main__":
    main()
