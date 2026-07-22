import cv2
import asyncio
import json
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.responses import JSONResponse
from ultralytics import YOLO
import logging
from datetime import datetime
from typing import Dict
import uvicorn

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="包裹视觉检测推理服务", version="1.0")

# 加载模型（当前目录下的 yolov8n.pt）
model = YOLO("yolov8n.pt")
logger.info("模型加载成功")

# 存储检测会话（暂未使用）
sessions: Dict[str, dict] = {}

# 循环检测开关（视频结束后自动重新开始）
LOOP_FOREVER = True

@app.websocket("/ws/detect")
async def websocket_detect(websocket: WebSocket):
    await websocket.accept()
    session_id = f"session_{len(sessions)}"
    cap = None
    try:
        data = await websocket.receive_text()
        config = json.loads(data)
        video_source = config.get("video_source")
        interval_sec = config.get("interval_sec", 1.0)
        conf_threshold = config.get("conf_threshold", 0.5)

        if not video_source:
            await websocket.send_json({"error": "video_source required"})
            await websocket.close()
            return

        # 循环检测主逻辑
        while True:
            # 打开视频源（若未打开或已结束）
            if cap is None or not cap.isOpened():
                logger.info(f"打开视频源: {video_source}")
                cap = cv2.VideoCapture(video_source)
                if not cap.isOpened():
                    await websocket.send_json({"error": "无法打开视频源"})
                    await websocket.close()
                    return
                fps = cap.get(cv2.CAP_PROP_FPS)
                interval_frames = max(1, int(fps * interval_sec)) if fps > 0 else 10
                frame_count = 0

            ret, frame = cap.read()
            if not ret:
                # 视频结束处理
                if LOOP_FOREVER:
                    logger.info("视频结束，重新打开循环")
                    cap.release()
                    cap = None
                    continue  # 重新进入循环，重新打开视频
                else:
                    break

            frame_count += 1
            if frame_count % interval_frames != 0:
                continue

            # 执行检测
            results = model(frame, conf=conf_threshold, verbose=False)[0]
            boxes = []
            for box in results.boxes:
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                h, w, _ = frame.shape
                boxes.append({
                    "class_id": int(box.cls[0]),
                    "class_name": model.names[int(box.cls[0])],
                    "confidence": float(box.conf[0]),
                    "x": x1 / w,
                    "y": y1 / h,
                    "width": (x2 - x1) / w,
                    "height": (y2 - y1) / h
                })

            await websocket.send_json({
                "frame_index": frame_count,
                "timestamp": datetime.now().isoformat(),
                "boxes": boxes
            })
            await asyncio.sleep(0.01)

        # 非循环模式下，发送结束状态
        await websocket.send_json({"status": "finished"})

    except WebSocketDisconnect:
        logger.info(f"WebSocket {session_id} disconnected")
    except Exception as e:
        logger.error(f"Error: {e}")
        await websocket.send_json({"error": str(e)})
    finally:
        if cap is not None:
            cap.release()
        if session_id in sessions:
            del sessions[session_id]

@app.post("/detect/start")
async def start_detection(video_source: str, interval_sec: float = 1.0, conf_threshold: float = 0.5, max_frames: int = 10):
    cap = cv2.VideoCapture(video_source)
    if not cap.isOpened():
        return JSONResponse(status_code=400, content={"error": "无法打开视频源"})

    fps = cap.get(cv2.CAP_PROP_FPS)
    interval_frames = max(1, int(fps * interval_sec)) if fps > 0 else 10
    results = []
    frame_count = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        frame_count += 1
        if frame_count % interval_frames != 0:
            continue
        results_model = model(frame, conf=conf_threshold, verbose=False)[0]
        boxes = []
        for box in results_model.boxes:
            x1, y1, x2, y2 = box.xyxy[0].tolist()
            h, w, _ = frame.shape
            boxes.append({
                "class_id": int(box.cls[0]),
                "class_name": model.names[int(box.cls[0])],
                "confidence": float(box.conf[0]),
                "x": x1 / w,
                "y": y1 / h,
                "width": (x2 - x1) / w,
                "height": (y2 - y1) / h
            })
        results.append({
            "frame_index": frame_count,
            "timestamp": cap.get(cv2.CAP_PROP_POS_MSEC) / 1000.0,
            "boxes": boxes
        })
        if len(results) >= max_frames:
            break
    cap.release()
    return {"code": 200, "data": results}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8001)