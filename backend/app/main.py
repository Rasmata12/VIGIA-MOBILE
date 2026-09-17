from __future__ import annotations

import logging
import re
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy import text

from app.config import get_settings
from app.db import engine, init_db
from app.engine import ai as ai_engine
from app.engine.url_engine import ENGINE_VERSION
from app.routers import (
    account, analysis, auth, before_pay, community, devices, guard, history, job_offer, listing, moment, privacy,
    shield, stats, verify,
)
from app.schemas import HealthOut

settings = get_settings()
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger("vigia")

SENSITIVE = re.compile(r"(password|token|api[_-]?key|authorization)", re.IGNORECASE)


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    if settings.jwt_secret_is_weak:
        if settings.is_production:
            # En production, un secret JWT faible ou par defaut permet de forger des tokens
            # d'authentification pour n'importe quel utilisateur : on refuse de demarrer.
            raise RuntimeError(
                "VIGIA_JWT_SECRET est absent, par defaut ou trop court (< 32 caracteres) "
                "alors que VIGIA_ENV=production. Genere un secret avec : "
                "python -c \"import secrets;print(secrets.token_urlsafe(64))\""
            )
        logger.warning("VIGIA_JWT_SECRET n'est pas configure : ne pas utiliser en production.")
    logger.info("VIGIA backend pret (env=%s, IA=%s, SafeBrowsing=%s, VirusTotal=%s)",
                settings.env, settings.has_ai, settings.has_safebrowsing, settings.has_virustotal)
    yield


app = FastAPI(
    title="VIGIA AI API",
    version="1.0.0",
    description="Backend d'analyse de liens et de messages pour l'application Android VIGIA AI.",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # l'app Android n'utilise pas CORS ; restreindre si un front web est ajoute
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def access_log(request: Request, call_next):
    response = await call_next(request)
    # Journalisation sans donnee sensible : ni contenu analyse, ni token.
    logger.info("%s %s -> %s", request.method, request.url.path, response.status_code)
    return response


@app.middleware("http")
async def security_headers(request: Request, call_next):
    response = await call_next(request)
    # En-tetes defensifs standards : coherents avec la mission de VIGIA (protection numerique).
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Referrer-Policy"] = "no-referrer"
    response.headers["Cache-Control"] = "no-store"
    return response


@app.exception_handler(Exception)
async def unhandled(request: Request, exc: Exception) -> JSONResponse:
    logger.exception("Erreur inattendue sur %s", request.url.path)
    return JSONResponse(status_code=500, content={"detail": "Erreur interne du serveur. Reessaie plus tard."})


@app.get("/health", response_model=HealthOut, tags=["system"])
async def health() -> HealthOut:
    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        db_status = "ok"
    except Exception:
        db_status = "unavailable"
    # Etat REEL de la couche IA (le modele repond-il vraiment ?), pas seulement "configuree" :
    # coherent avec la regle du projet de ne jamais afficher une fonctionnalite comme active
    # sans l'avoir verifie.
    ai_reachable, ai_detail = await ai_engine.ai_reachable()
    return HealthOut(
        status="ok" if db_status == "ok" else "degraded",
        engine_version=ENGINE_VERSION,
        database=db_status,
        ai_enabled=ai_reachable,
        ai_detail=ai_detail if settings.has_ai else "aucun modele configure",
        safebrowsing_enabled=settings.has_safebrowsing,
        virustotal_enabled=settings.has_virustotal,
        network_probes=settings.allow_network_probes,
    )


app.include_router(auth.router)
app.include_router(analysis.router)
app.include_router(history.router)
app.include_router(stats.router)
app.include_router(account.router)
app.include_router(verify.router)
app.include_router(guard.router)
app.include_router(moment.router)
app.include_router(before_pay.router)
app.include_router(job_offer.router)
app.include_router(community.router)
app.include_router(listing.router)
app.include_router(shield.router)
app.include_router(privacy.router)
app.include_router(devices.router)
