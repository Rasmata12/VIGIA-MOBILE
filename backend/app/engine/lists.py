"""Listes de reference embarquees (fonctionnent hors ligne)."""

SHORTENERS = {
    "bit.ly", "tinyurl.com", "goo.gl", "t.co", "ow.ly", "is.gd", "buff.ly", "cutt.ly",
    "rb.gy", "shorturl.at", "rebrand.ly", "tiny.cc", "bl.ink", "s.id", "urlz.fr",
    "lnkd.in", "t.ly", "shrtco.de", "short.gy", "v.gd", "qr.ae",
}

# TLD statistiquement tres exposees a l'abus (rapports Spamhaus / Interisle)
HIGH_RISK_TLDS = {
    "zip", "mov", "top", "xyz", "gq", "cf", "tk", "ml", "ga", "work", "click", "link",
    "country", "kim", "loan", "men", "party", "review", "rest", "cam", "sbs", "cfd",
    "bond", "quest", "buzz", "monster", "icu", "support", "fit", "autos", "lol",
}

FREE_HOSTING = {
    "000webhostapp.com", "weeblysite.com", "glitch.me", "repl.co", "vercel.app",
    "netlify.app", "pages.dev", "web.app", "firebaseapp.com", "herokuapp.com",
    "github.io", "blogspot.com", "wixsite.com", "r2.dev", "workers.dev", "duckdns.org",
    "serveo.net", "ngrok.io", "ngrok-free.app", "trycloudflare.com",
}

# Marques usurpees + operateurs mobile money (Afrique de l'Ouest inclus)
BRANDS = {
    "paypal", "microsoft", "outlook", "office365", "apple", "icloud", "google",
    "gmail", "facebook", "instagram", "whatsapp", "netflix", "amazon", "dhl",
    "fedex", "chronopost", "laposte", "ups", "impots", "ameli", "caf", "orange",
    "sfr", "bouygues", "free", "bnpparibas", "societegenerale", "creditagricole",
    "revolut", "binance", "coinbase", "metamask", "trustwallet", "ledger",
    "orangemoney", "moovmoney", "mtn", "mtnmomo", "wave", "ecobank", "coris",
    "uba", "boa", "sonabel", "onatel", "western", "westernunion", "moneygram",
}

SENSITIVE_PATH_WORDS = {
    "login", "signin", "log-in", "verify", "verification", "secure", "security",
    "account", "update", "confirm", "password", "webscr", "recover", "unlock",
    "billing", "payment", "invoice", "wallet", "seed", "recovery-phrase", "kyc",
    "connexion", "identifiant", "motdepasse", "verifier", "paiement", "facture",
    "compte", "securite", "mise-a-jour",
}

DANGEROUS_EXTENSIONS = {
    ".apk", ".exe", ".msi", ".scr", ".bat", ".cmd", ".jar", ".vbs", ".ps1",
    ".dmg", ".iso", ".hta", ".js", ".lnk",
}

# Messageries grand public : un vrai recruteur d'entreprise ecrit depuis un domaine
# d'entreprise, pas depuis une adresse personnelle gratuite.
FREE_EMAIL_DOMAINS = {
    "gmail.com", "yahoo.com", "yahoo.fr", "hotmail.com", "hotmail.fr", "outlook.com",
    "outlook.fr", "live.com", "icloud.com", "aol.com", "gmx.com", "mail.com",
    "protonmail.com", "yandex.com",
}
