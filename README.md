# VIGIA AI — projet fusionné (backend renforcé + interface Android)

Ce paquet réunit :

- **`backend/`** — la version renforcée de ton backend FastAPI (auth, analyses, historique,
  stats, Before Pay, Guard, Moment & Shield, **offres d'emploi**, **petites annonces**,
  **espace communautaire**, confidentialité RGPD, appareils).
- **`android/`** — l'interface Kotlin / Jetpack Compose que tu avais déjà (thème premium bleu
  proche de ton logo), **améliorée** :
  - icône de l'application et écran de connexion mis à jour avec ton vrai logo VIGIA AI ;
  - icône adaptative Android (fond blanc + logo détouré, rendu net sur tous les lanceurs) ;
  - **3 nouveaux écrans** ajoutés pour coller aux modules du backend renforcé, absents de
    l'interface d'origine :
    - `JobOfferScreen` — vérifier une offre d'emploi / formation (`/job-offer`)
    - `ListingScreen` — vérifier une petite annonce (`/listing`)
    - `CommunityScreen` — signaler une cible ou vérifier avant d'agir (`/community/reports`,
      `/community/check`)
  - ces écrans sont accessibles depuis une nouvelle section **« Autres Protections »** sur
    l'accueil, dans le même style que le reste de l'app (GlassCard, RiskGauge, ScamDnaCard,
    DecisionBanner...).
  - DTOs et endpoints Retrofit ajoutés dans `net/Dtos.kt` et `net/ApiService.kt`, ViewModels
    ajoutés dans `ui/vm/ViewModels.kt`, en suivant exactement le pattern déjà utilisé par
    `BeforePayScreen`/`BeforePayViewModel`.

## L'app ne parle qu'à CE backend

- `android/gradle.properties` → `VIGIA_API_BASE_URL=https://vigia-mobile.onrender.com/` (API
  HTTPS du backend déployé; `/health` répond actuellement `status: ok`).
- `android/app/src/main/res/xml/network_security_config.xml` interdit tout trafic non chiffré
  **sauf** vers `10.0.2.2` / `localhost` pour le développement local. L'application de
  production utilise le backend HTTPS ci-dessus; aucune autre adresse n'est autorisée par le code.
- Aucun autre client HTTP, aucune autre base URL, aucun SDK tiers d'analyse n'est présent dans
  le code : tout passe par `ApiService` / `HttpClient.kt` vers cette unique API.

## Lancer le tout en local

```bash
# 1. Backend
cd backend
./run.sh                      # http://localhost:8000 (Swagger sur /docs)

# 2. Android
# Ouvrir android/ dans Android Studio, lancer sur un émulateur (10.0.2.2 pointera
# automatiquement vers le backend qui tourne sur ta machine).
```

## Important — compilation de l'APK

Je ne peux pas compiler un `.apk` binaire dans cet environnement (pas de SDK Android/Gradle
accessible ici). Le code source est complet et structuré pour compiler tel quel avec
`./gradlew assembleDebug` (ou directement via Android Studio → Build → Build APK(s)).

## Ce qui reste à faire de ton côté

1. `cd backend && ./run.sh` (crée `.env` avec une vraie clé JWT au premier lancement).
2. Ouvrir `android/` dans Android Studio, laisser Gradle synchroniser les dépendances.
3. Lancer sur un émulateur — connexion, puis test des nouveaux écrans (Offre d'emploi,
   Petite annonce, Communauté) depuis la section « Autres Protections » de l'accueil.
4. Pour un vrai déploiement : héberger le backend (HTTPS obligatoire), mettre l'URL dans
   `VIGIA_API_BASE_URL`, générer un keystore de signature release (voir `INSTALLATION.md`).
