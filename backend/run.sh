#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
[ -d .venv ] || python3 -m venv .venv
source .venv/bin/activate
pip install -q -r requirements.txt
[ -f .env ] || { cp .env.example .env
  SECRET=$(python -c "import secrets;print(secrets.token_urlsafe(64))")
  sed -i.bak "s|VIGIA_JWT_SECRET=.*|VIGIA_JWT_SECRET=$SECRET|" .env && rm -f .env.bak
  echo "Fichier .env cree avec une cle JWT generee."; }
exec uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
