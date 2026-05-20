import shutil

WEIGHTS_PATH = "weights/fastdvdnet.pth"


class FastDVDnet:
    def __init__(self):
        # ── 더미 모드 (가중치 전달 전) ──────────────────────────────
        self.model = None
        print("[FastDVDnet] 더미 모드로 로드됨")

        # ── 실제 모드 (가중치 전달 후: 위 두 줄 주석, 아래 주석 해제) ──
        # import torch
        # from .fastdvdnet_arch import FastDVDnet as Net  # 모델팀 구조 파일
        # self.model = Net()
        # self.model.load_state_dict(torch.load(WEIGHTS_PATH))
        # self.model.eval()
        # print(f"[FastDVDnet] 가중치 로드 완료: {WEIGHTS_PATH}")

    def infer(self, input_path: str, output_path: str) -> None:
        # ── 더미 모드: 입력 파일을 그대로 복사 ──────────────────────
        shutil.copy(input_path, output_path)
        print(f"[FastDVDnet] 더미 추론: {input_path} → {output_path}")

        # ── 실제 모드 (가중치 전달 후: 위 두 줄 주석, 아래 주석 해제) ──
        # import torch
        # result = self.model(torch.load(input_path))
        # torch.save(result, output_path)
