from __future__ import annotations

import hashlib
import json
import time

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.schemas import MediaAnalysisOut
from app.security import current_user, rate_limit
from app.engine import ai as ai_engine
from app.models import Alert, Analysis

router = APIRouter(prefix="/media", tags=["media"] )

MAX_FRAME_BYTES = 6 * 1024 * 1024
MAX_TOTAL_BYTES = 24 * 1024 * 1024
ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"}

@router.post("/analyze", response_model=MediaAnalysisOut)
async def analyze_media(
    frames: list[UploadFile] = File(...),
    media_type: str = Form(...),
    filename: str = Form(default="media"),
    user = Depends(current_user),
    db: Session = Depends(get_db),
):
    if media_type not in {"image", "video"}:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "Type de média non supporté.")
    if len(frames) < 1 or len(frames) > 6:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "Le média doit fournir entre 1 et 6 images.")
    rate_limit(db, f"media:{user.id}", limit=30, window_seconds=3600)
    packed: list[tuple[bytes, str]] = []
    total_bytes = 0
    for frame in frames:
        if frame.content_type and frame.content_type not in ALLOWED_IMAGE_TYPES:
            raise HTTPException(status.HTTP_415_UNSUPPORTED_MEDIA_TYPE, "Seules les images JPEG, PNG ou WebP sont acceptées.")
        data = await frame.read()
        total_bytes += len(data)
        if not data or len(data) > MAX_FRAME_BYTES:
            raise HTTPException(status.HTTP_413_REQUEST_ENTITY_TOO_LARGE, "Une image du média dépasse 6 Mo.")
        if total_bytes > MAX_TOTAL_BYTES:
            raise HTTPException(status.HTTP_413_REQUEST_ENTITY_TOO_LARGE, "Le lot d'images dépasse 24 Mo.")
        mime = frame.content_type if frame.content_type in ALLOWED_IMAGE_TYPES else "image/jpeg"
        packed.append((data, mime))
    started = time.perf_counter()
    try:
        result = await ai_engine.analyse_media(packed, media_type, filename[:160])
    except ValueError as exc:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, str(exc))
    except RuntimeError as exc:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, str(exc))
    except Exception:
        raise HTTPException(status.HTTP_502_BAD_GATEWAY, "Le modèle vision Hugging Face n'a pas répondu correctement.")

    digest = hashlib.sha256(b"".join(data for data, _ in packed)).hexdigest()
    signals = [{"code": "vision_indicator", "label": label, "weight": 0, "evidence": "Analyse vision", "category": "ia"} for label in result["indicators"]]
    record = Analysis(
        user_id=user.id, kind="media", input_preview=filename[:280], input_sha256=digest,
        score=result["score"], level=result["level"], summary=result["summary"],
        signals_json=json.dumps(signals, ensure_ascii=False),
        sources_json=json.dumps([
            {"name": "Hugging Face Vision", "status": "ok", "detail": result["model"]},
            {"name": "Corroboration VIGIA", "status": "ok" if result.get("corroborating_score", 0) else "not_applicable",
             "detail": f"score secondaire {result.get('corroborating_score', 0)}/100"},
        ], ensure_ascii=False),
        ai_used=True, confidence=80, module="verify",
        assessment_json=json.dumps(result, ensure_ascii=False), engine_version="media-1.0",
        duration_ms=int((time.perf_counter() - started) * 1000),
    )
    db.add(record)
    if result["level"] in {"dangerous", "suspicious"}:
        db.add(Alert(
            user_id=user.id, analysis_id=record.id,
            title="Menace visuelle détectée" if result["level"] == "dangerous" else "Contenu visuel suspect",
            body=result["summary"][:500], level=result["level"], source="verify",
            reason=" | ".join(result["indicators"][:3]),
            recommended_action=(result["recommended_actions"] or ["Ne partagez pas ce contenu et vérifiez sa source."])[0],
        ))
    db.commit()
    return MediaAnalysisOut(**result)
