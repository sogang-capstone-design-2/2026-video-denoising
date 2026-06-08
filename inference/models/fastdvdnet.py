import os
import requests

INTENSITY_TO_SIGMA = {"low": 10, "medium": 25, "high": 40}


class FastDVDnet:
    def __init__(self):
        self.api_url = os.getenv("INFERENCE_BASE_URL")
        self.api_key = os.getenv("INFERENCE_API_KEY")
        if self.api_url and self.api_key:
            print(f"[FastDVDnet] 외부 API 모드: {self.api_url}")
        else:
            print("[FastDVDnet] 경고: API URL/KEY 미설정 — 더미 모드로 동작")

    def infer(self, input_path: str, output_path: str, intensity: str = "medium") -> None:
        if not self.api_url or not self.api_key:
            # 더미 모드
            import shutil
            shutil.copy(input_path, output_path)
            print(f"[FastDVDnet] 더미 추론: {input_path} → {output_path}")
            return

        noise_sigma = INTENSITY_TO_SIGMA.get(intensity, 25)

        with open(input_path, "rb") as f:
            response = requests.post(
                f"{self.api_url}/denoise",
                headers={"X-API-Key": self.api_key},
                files={"file": f},
                data={"noise_sigma": noise_sigma},
                timeout=120,
            )

        response.raise_for_status()

        with open(output_path, "wb") as f:
            f.write(response.content)

        print(f"[FastDVDnet] 추론 완료: {output_path} "
              f"(프레임: {response.headers.get('X-Frames')}, "
              f"처리시간: {response.headers.get('X-Elapsed-Seconds')}s)")
