# VIGIA AI — Installation et exécution

## 1. Backend (obligatoire pour les analyses complètes)

```bash
cd backend
./run.sh          # crée le venv, installe, génère la clé JWT, lance sur le port 8000
```
ou manuellement :
```bash
cd backend
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
python -c "import secrets;print(secrets.token_urlsafe(64))"   # colle le résultat dans VIGIA_JWT_SECRET
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Vérification : `curl http://127.0.0.1:8000/health` → `{"status":"ok", ...}`
Documentation interactive de l'API : `http://127.0.0.1:8000/docs`

Tests : `python -m pytest tests -q` (16 tests couvrant compte, isolation, analyse, historique, sécurité).

### Base de données
- Développement : SQLite, créée automatiquement (`vigia.db`), aucune action requise.
- Production : PostgreSQL. Mettre dans `.env` :
  `VIGIA_DATABASE_URL=postgresql+psycopg://utilisateur:motdepasse@hote:5432/vigia`
  Les tables sont créées au démarrage.

### Clés API (facultatives, mais ce sont elles qui activent l'IA et la réputation)
| Variable | Où l'obtenir | Ce que ça active |
|---|---|---|
| `VIGIA_ANTHROPIC_API_KEY` | console.anthropic.com | Explication et score générés par un vrai modèle |
| `VIGIA_GOOGLE_SAFEBROWSING_KEY` | Google Cloud → API Safe Browsing v4 | Vérification des liens dans la base Google |
| `VIGIA_VIRUSTOTAL_KEY` | virustotal.com/gui/my-apikey | Verdicts de ~90 moteurs antivirus |

Sans ces clés, l'application fonctionne mais l'écran Réglages affiche « non configuré »
et aucune réponse IA n'est inventée.

### Docker
```bash
cd backend && docker build -t vigia-api .
docker run -p 8000:8000 --env-file .env vigia-api
```

## 2. Application Android

1. Ouvrir le dossier `android/` dans **Android Studio** (Ladybug ou plus récent, JDK 17).
   Android Studio télécharge Gradle et le SDK automatiquement au premier ouvrage.
2. Renseigner l'adresse du backend dans `android/gradle.properties` :
   - émulateur + backend local : `VIGIA_API_BASE_URL=http://10.0.2.2:8000/`
   - téléphone réel sur le même Wi-Fi : `http://192.168.X.X:8000/` (et ajouter cette IP
     dans `app/src/main/res/xml/network_security_config.xml`)
   - production : `https://api.tondomaine.com/` (HTTPS obligatoire, aucune exception à ajouter)
3. Lancer : bouton ▶ (Run).

### Générer l'APK
```bash
cd android
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk — installable directement
./gradlew testDebugUnitTest    # tests du moteur local
```
Si `gradlew` est absent (le wrapper binaire n'est pas inclus dans cette archive) :
ouvre le projet une fois dans Android Studio, ou exécute `gradle wrapper --gradle-version 8.9`.

### Version release signée
```bash
keytool -genkey -v -keystore vigia-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias vigia
```
Ajoute dans `android/app/build.gradle.kts` un bloc `signingConfigs` pointant vers ce keystore
(chemin et mots de passe via variables d'environnement, jamais en clair dans le dépôt), puis :
```bash
./gradlew assembleRelease
```

## 3. Installer sur un téléphone
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
ou transfère l'APK sur le téléphone et autorise l'installation depuis cette source.

## 4. Fonction « Partager avec VIGIA »
Une fois l'app installée : dans WhatsApp, SMS, Gmail ou le navigateur,
sélectionne le message → **Partager** → **VIGIA AI**.
Une fiche s'ouvre par-dessus l'application d'origine et analyse immédiatement le contenu.
Le même point d'entrée est disponible via le menu de sélection de texte (« Analyser avec VIGIA »).

## 5. Générer l'APK automatiquement avec GitHub (CI/CD)

Le dépôt contient `.github/workflows/build-apk.yml`. Il compile l'APK sur les serveurs
de GitHub — aucun Android Studio nécessaire sur ta machine — et fonctionne même sans
le wrapper `gradlew` (l'action `gradle/actions/setup-gradle` installe Gradle elle-même).

**Étape 1 — Déployer le backend pour avoir une vraie URL HTTPS (Render) :**
1. Sur [render.com](https://render.com) → **New +** → **Web Service** → connecte ton dépôt GitHub,
   ou choisis "Deploy an existing image" si tu préfères construire l'image Docker toi-même.
2. Dossier racine (Root Directory) : `backend` — c'est là que se trouve le `Dockerfile`.
3. Render détecte le `Dockerfile` et propose "Docker" comme environnement : laisse tel quel.
4. Dans **Environment → Environment Variables**, ajoute au minimum :
   | Variable | Valeur |
   |---|---|
   | `VIGIA_ENV` | `production` |
   | `VIGIA_JWT_SECRET` | un secret généré avec `python -c "import secrets;print(secrets.token_urlsafe(64))"` (32+ caractères, sinon le serveur refuse de démarrer en production) |
   | `VIGIA_DATABASE_URL` | laisse vide pour SQLite (données perdues à chaque redéploiement — ok pour tester), ou l'URL d'une base **PostgreSQL** Render (`postgresql+psycopg://...`) pour la vraie prod |

   Facultatif : `VIGIA_GOOGLE_SAFEBROWSING_KEY`, `VIGIA_VIRUSTOTAL_KEY`, `VIGIA_OLLAMA_BASE_URL` / `VIGIA_OC_BASE_URL` + `VIGIA_OC_API_KEY` si tu veux activer la réputation externe ou l'IA (voir tableau plus haut).
5. Déploie. Render te donne une URL du type `https://vigia-backend-xxxx.onrender.com`.
6. Vérifie : `curl https://vigia-backend-xxxx.onrender.com/health` → doit répondre `{"status":"ok", ...}`.
   (Le plan gratuit Render met le service en veille après inactivité : le premier appel peut
   prendre ~30s le temps qu'il se réveille — normal, pas une panne.)

**Étape 2 — Faire pointer l'APK vers cette URL :**
- Option A (la plus simple) : onglet **Actions** du dépôt GitHub → sélectionne
  **Build VIGIA APK** → **Run workflow** → colle ton URL Render (avec le `/` final) dans
  le champ `api_base_url` → **Run workflow**.
- Option B : modifie la valeur par défaut dans `.github/workflows/build-apk.yml`
  (`default: "https://..."`) et dans `android/gradle.properties`, puis pousse sur `main` :
  le build se déclenche automatiquement à chaque push touchant `android/`.

**Étape 3 — Récupérer l'APK :**
Une fois le workflow terminé (icône verte), ouvre son résumé → section **Artifacts** en bas
de page → télécharge `vigia-ai-debug-apk` (un `.zip` contenant `app-debug.apk`).
Transfère ce fichier sur un téléphone Android et autorise l'installation depuis cette source
("Installer des applications inconnues"), ou envoie-le toi-même via `adb install`.

Cet APK de debug est auto-signé par Android (clé de debug) : parfait pour tester sur ton
téléphone ou le partager à quelques personnes, mais le Play Store exige un APK/AAB **release**
signé avec ta propre clé — voir la section précédente pour générer un keystore, puis on peut
étendre ce workflow pour signer automatiquement (avec le keystore stocké en secret GitHub)
si tu veux aller jusque-là.

