import asyncio
import os
import tempfile
from contextlib import asynccontextmanager
from dotenv import load_dotenv
load_dotenv()
from fastapi import FastAPI, File, Form, UploadFile, HTTPException
from fastapi.responses import FileResponse
from starlette.background import BackgroundTask

from models.fastdvdnet import FastDVDnet
from models.rvdenet import RViDeNet

models: dict = {}


@asynccontextmanager
async def lifespan(app: FastAPI):
    print("모델 로딩 중...")
    models["general"] = FastDVDnet()
    models["lowlight"] = RViDeNet()
    print("모델 로딩 완료")
    yield
    models.clear()


app = FastAPI(title="AI Driven Night Vision - Inference Server", lifespan=lifespan)


# ── 엔드포인트 ───────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/infer")
async def infer(
    file: UploadFile = File(...),
    mode: str = Form(...),
    intensity: str = Form("medium"),
):
    if mode not in models:
        raise HTTPException(status_code=400, detail=f"지원하지 않는 모드: {mode}")

    ext = os.path.splitext(file.filename)[1] if file.filename else ".bin"

    # 임시 파일에 업로드 파일 저장
    with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as src:
        src.write(await file.read())
        input_path = src.name

    output_path = input_path + "_denoised" + ext

    def cleanup():
        for path in (input_path, output_path):
            if os.path.exists(path):
                os.remove(path)

    try:
        loop = asyncio.get_event_loop()
        await loop.run_in_executor(
            None, lambda: models[mode].infer(input_path, output_path, intensity)
        )
        return FileResponse(
            path=output_path,
            media_type="application/octet-stream",
            filename=f"denoised_{file.filename}",
            background=BackgroundTask(cleanup),
        )
    except Exception as e:
        cleanup()
        raise HTTPException(status_code=500, detail=str(e))
