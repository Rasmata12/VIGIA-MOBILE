from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="VIGIA_", env_file=".env", extra="ignore")

    env: str = "development"  # "development" | "production" — force la robustesse du secret en prod

    jwt_secret: str = "dev-insecure-secret-change-me"
    jwt_algorithm: str = "HS256"
    access_token_minutes: int = 30
    refresh_token_days: int = 60

    database_url: str = "sqlite:///./vigia.db"

    google_safebrowsing_key: str = ""
    virustotal_key: str = ""

    # --- Couche IA : jamais d'API payante par defaut -----------------------
    # "ollama"            -> modele local reel (Llama, Qwen, Mistral...) execute par Ollama,
    #                        gratuit et illimite, et rien ne quitte le serveur (ideal pour une
    #                        app de protection numerique : le contenu de l'utilisateur ne part
    #                        jamais vers un tiers).
    # "openai_compatible" -> pour brancher une API gratuite tierce compatible (ex : niveau
    #                        gratuit de Groq/OpenRouter) si tu preferes ne pas heberger de modele.
    # "none"               -> IA desactivee, l'heuristique seule fait foi (comportement honnete,
    #                        jamais de resultat invente).
    ai_provider: str = "ollama"

    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "llama3.2"

    oc_base_url: str = ""       # ex: https://api.groq.com/openai/v1
    oc_api_key: str = ""
    oc_model: str = ""

    # Hugging Face Inference Providers — le jeton reste exclusivement côté serveur.
    hf_token: str = ""
    hf_model: str = "openai/gpt-oss-120b:fastest"
    hf_vision_model: str = "Qwen/Qwen2.5-VL-3B-Instruct"
    hf_base_url: str = "https://router.huggingface.co/v1"

    ai_request_timeout: float = 60.0

    allow_network_probes: bool = True
    rate_limit_enabled: bool = True
    http_timeout: float = 6.0

    @property
    def has_safebrowsing(self) -> bool:
        return bool(self.google_safebrowsing_key.strip())

    @property
    def has_virustotal(self) -> bool:
        return bool(self.virustotal_key.strip())

    @property
    def has_ai(self) -> bool:
        """Vrai si une couche IA est CONFIGUREE. Ne garantit pas qu'elle soit JOIGNABLE :
        voir app.engine.ai.ai_reachable() pour un etat en direct (utilise par /health)."""
        provider = self.ai_provider.strip().lower()
        if provider == "ollama":
            return bool(self.ollama_base_url.strip() and self.ollama_model.strip())
        if provider == "openai_compatible":
            return bool(self.oc_base_url.strip() and self.oc_model.strip())
        if provider == "huggingface":
            return bool(self.hf_token.strip() and self.hf_model.strip())
        return False

    @property
    def ai_label(self) -> str:
        provider = self.ai_provider.strip().lower()
        if provider == "ollama":
            return f"modele local ({self.ollama_model} via Ollama, sur le serveur, rien n'est envoye a un tiers)"
        if provider == "openai_compatible":
            return f"{self.oc_model} (API compatible OpenAI configuree en {self.oc_base_url})"
        if provider == "huggingface":
            return f"{self.hf_model} (Hugging Face Inference Providers, configuré sur le serveur)"
        return "aucune (heuristique seule)"

    @property
    def is_production(self) -> bool:
        return self.env.strip().lower() == "production"

    @property
    def jwt_secret_is_weak(self) -> bool:
        # Le secret par defaut, ou un secret trop court, ne resiste pas a une attaque par force brute
        # sur les jetons signes en HS256. On exige au moins 32 caracteres en production.
        return self.jwt_secret == "dev-insecure-secret-change-me" or len(self.jwt_secret) < 32


@lru_cache
def get_settings() -> Settings:
    return Settings()
