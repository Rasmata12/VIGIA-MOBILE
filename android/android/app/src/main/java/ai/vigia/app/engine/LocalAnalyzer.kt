package ai.vigia.app.engine

import java.util.Locale

/**
 * Moteur d'analyse LOCAL, execute entierement sur le telephone.
 * Il fonctionne sans connexion et sans serveur. Ce n'est pas une simulation :
 * chaque signal provient d'une regle appliquee au contenu reel soumis.
 * Le moteur serveur (reseau, TLS, reputation, IA) va plus loin ; quand la
 * connexion est disponible, c'est son resultat qui est affiche.
 */
object LocalAnalyzer {

    const val VERSION = "1.0.0"

    data class Signal(val code: String, val label: String, val weight: Int, val evidence: String = "")
    data class Result(
        val score: Int,
        val level: String,
        val summary: String,
        val signals: List<Signal>,
        val urls: List<String>
    )

    private val URL_REGEX = Regex("""(?:https?://|www\.)[^\s<>"']+""", RegexOption.IGNORE_CASE)

    private val SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "goo.gl", "t.co", "ow.ly", "is.gd", "cutt.ly",
        "rb.gy", "shorturl.at", "t.ly", "s.id", "urlz.fr", "short.gy"
    )
    private val RISKY_TLDS = setOf(
        "zip", "top", "xyz", "gq", "cf", "tk", "ml", "ga", "click", "link", "icu",
        "cam", "sbs", "cfd", "quest", "buzz", "monster", "lol", "fit", "rest", "work"
    )
    private val BRANDS = setOf(
        "paypal", "microsoft", "apple", "icloud", "google", "gmail", "facebook",
        "instagram", "whatsapp", "netflix", "amazon", "dhl", "orange", "moov",
        "mtn", "wave", "ecobank", "coris", "binance", "coinbase", "westernunion"
    )
    private val SENSITIVE_WORDS = setOf(
        "login", "signin", "verify", "secure", "account", "update", "confirm",
        "password", "wallet", "seed", "kyc", "connexion", "motdepasse", "paiement", "compte"
    )

    private data class Rule(val code: String, val label: String, val weight: Int, val patterns: List<Regex>)

    private fun rx(vararg p: String) = p.map { Regex(it, RegexOption.IGNORE_CASE) }

    private val TEXT_RULES = listOf(
        Rule("urgence", "Le message crée un sentiment d'urgence pour t'empêcher de réfléchir.", 16,
            rx("""\burgent\b""", "imm[ée]diatement", """sous \d+\s*(h|heures|minutes)""",
                "derni[eè]re? (chance|avertissement|rappel)", """\bexpire\b""", "agir maintenant")),
        Rule("menace", "Le message menace de suspendre, bloquer ou supprimer quelque chose.", 20,
            rx("suspend(u|re|ue)", "bloqu[ée]", "d[ée]sactiv[ée]", "supprim[ée]", "amende", "sanction", "votre compte sera")),
        Rule("identifiants", "Le message demande des identifiants, un code ou un mot de passe.", 34,
            rx("mot de passe", "identifiant", """\bpassword\b""", "code (secret|pin|confidentiel)",
                """\bpin\b""", "code de v[ée]rification", """\botp\b""", """\bcvv\b""",
                "num[ée]ro de carte", "phrase de r[ée]cup[ée]ration", "seed phrase")),
        Rule("paiement", "Le message demande un paiement, un transfert ou des frais.", 24,
            rx("frais de (dossier|livraison|douane|activation|deblocage)", "virement", "transf[ée]rez?",
                "mobile money", "orange money", "moov money", "mtn momo", "recharge",
                "gift ?card", "bitcoin", """\busdt\b""", "western union", "moneygram")),
        Rule("recompense", "Le message promet un gain, un cadeau ou un remboursement inattendu.", 22,
            rx("f[ée]licitations", "vous avez gagn", """\bgagnant\b""", "tirage", "loterie",
                "remboursement", "h[ée]ritage", """rendement de \d+ ?%""", "investissement garanti")),
        Rule("lien_action", "Le message pousse à cliquer sur un lien pour régler un problème.", 12,
            rx("cliquez? (ici|sur ce lien)", "suivez? ce lien", "click here",
                "confirmez? (votre|vos)", "v[ée]rifiez? (votre|vos)", "mettre a jour vos")),
        Rule("secret", "Le message demande de garder le secret ou de ne rien vérifier ailleurs.", 26,
            rx("ne (dites|parlez) (rien|a personne)", "restez? discret", "confidentiel", "entre nous", "ne contactez pas")),
        Rule("autorite", "Le message se fait passer pour une autorité ou un service officiel.", 14,
            rx("service (client|fiscal|des imp[oô]ts)", "police", "gendarmerie", "minist[eè]re",
                """\bdouane\b""", "tribunal", "huissier", "votre banque", "support technique")),
        Rule("livraison", "Le message évoque un colis bloqué, motif de phishing très courant.", 16,
            rx("colis (en attente|bloqu[ée]|non livr[ée])", "frais de livraison", """\bdhl\b""", "votre (commande|livraison)")),
        Rule("hors_canal", "Le message renvoie vers WhatsApp ou Telegram, hors des canaux officiels.", 12,
            rx("contactez?[- ]nous sur whatsapp", "telegram", """t\.me/""", """wa\.me/"""))
    )

    fun analyzeText(raw: String): Result {
        val text = raw.trim()
        val signals = mutableListOf<Signal>()
        for (rule in TEXT_RULES) {
            val hits = rule.patterns.mapNotNull { it.find(text)?.value }
            if (hits.isNotEmpty()) {
                val bonus = minOf(hits.size - 1, 2) * 3
                signals += Signal(rule.code, rule.label, rule.weight + bonus, hits.distinct().joinToString(", ").take(140))
            }
        }
        val letters = text.filter { it.isLetter() }
        if (letters.length >= 40 && letters.count { it.isUpperCase() }.toDouble() / letters.length > 0.45) {
            signals += Signal("majuscules", "Le message est écrit majoritairement en majuscules, procédé typique des arnaques.", 8)
        }
        if (text.count { it == '!' } >= 4) {
            signals += Signal("ponctuation", "Ponctuation excessive, conçue pour faire réagir vite.", 6)
        }
        val brands = BRANDS.filter { Regex("""\b$it\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) }
        if (brands.isNotEmpty()) {
            signals += Signal("marque", "Le message se réclame de : ${brands.take(3).joinToString(", ")}. Vérifie toujours via l'application officielle.", 8)
        }

        val urls = URL_REGEX.findAll(text).map { it.value.trimEnd('.', ',', ')', '»') }.distinct().take(5).toList()
        for (url in urls) {
            val urlResult = analyzeUrl(url)
            val weight = when (urlResult.level) { "dangerous" -> 40; "suspicious" -> 20; else -> 0 }
            signals += Signal("lien_${urlResult.level}", "Lien contenu dans le message : ${urlResult.summary}", weight, url.take(120))
        }

        val score = signals.sumOf { it.weight }.coerceIn(0, 100)
        return Result(score, levelOf(score), summarizeText(levelOf(score), signals), signals, urls)
    }

    fun analyzeUrl(raw: String): Result {
        val normalized = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(raw)) raw else "http://$raw"
        val signals = mutableListOf<Signal>()
        val uri = try { java.net.URI(normalized) } catch (e: Exception) { null }
        val host = uri?.host?.lowercase(Locale.ROOT).orEmpty()
        if (host.isBlank()) {
            return Result(0, "invalid", "Cette adresse n'est pas un lien valide et n'a pas pu être analysée.", emptyList(), emptyList())
        }
        val domain = registrableDomain(host)
        val tld = host.substringAfterLast('.', "")
        val pathQuery = ((uri?.path ?: "") + "?" + (uri?.query ?: "")).lowercase(Locale.ROOT)

        if (normalized.startsWith("https://")) signals += Signal("https", "La connexion est chiffrée (HTTPS).", -5)
        else signals += Signal("http", "Le lien n'est pas chiffré (HTTP) : tout ce qui est saisi circule en clair.", 18)

        if (Regex("""^\d{1,3}(\.\d{1,3}){3}$""").matches(host)) {
            signals += Signal("ip", "Le lien pointe vers une adresse IP brute : typique des pages frauduleuses temporaires.", 30, host)
        }
        if (normalized.substringAfter("//").substringBefore("/").contains("@")) {
            signals += Signal("arobase", "Le lien contient un '@' : ce qui est affiché avant masque la vraie destination.", 35)
        }
        if (host.startsWith("xn--") || host.contains(".xn--")) {
            signals += Signal("punycode", "Le domaine utilise un encodage Punycode, souvent pour imiter un domaine légitime.", 32, host)
        }
        if (host.split(".").size >= 5) {
            signals += Signal("sous_domaines", "Le domaine empile de nombreux sous-domaines pour noyer la vraie destination.", 14, host)
        }
        if (domain in SHORTENERS) {
            signals += Signal("raccourci", "Lien raccourci : la destination réelle reste masquée tant qu'on ne l'ouvre pas.", 22, domain)
        }
        if (tld in RISKY_TLDS) {
            signals += Signal("extension", "L'extension .$tld est parmi les plus utilisées pour héberger des pages malveillantes.", 18, tld)
        }
        val core = domain.substringBefore(".")
        for (brand in BRANDS) {
            if (core == brand) break
            val distance = levenshtein(core, brand)
            if (distance in 1..2 && core.length >= 5 && kotlin.math.abs(core.length - brand.length) <= 2) {
                signals += Signal("typosquat", "Le domaine '$core' imite la marque '$brand' à $distance caractère(s) près.", 40, domain); break
            }
            if (brand.length >= 5 && host.contains(brand) && !core.contains(brand)) {
                signals += Signal("marque_sous_domaine", "Le nom '$brand' est placé dans le sous-domaine mais le vrai domaine est '$domain'.", 38, host); break
            }
        }
        val words = SENSITIVE_WORDS.filter { pathQuery.contains(it) }
        if (words.isNotEmpty()) {
            signals += Signal("mots_sensibles", "L'adresse contient des mots liés aux comptes ou aux paiements (${words.take(3).joinToString(", ")}).", 10 + 4 * minOf(words.size, 3))
        }
        listOf(".apk", ".exe", ".msi", ".scr", ".jar", ".bat").firstOrNull { pathQuery.contains(it) }?.let {
            signals += Signal("fichier", "Le lien télécharge un fichier $it : ne jamais l'installer depuis une source non vérifiée.", 45, it)
        }
        if (normalized.length > 120) signals += Signal("longueur", "Le lien est anormalement long (${normalized.length} caractères).", 8)

        val score = signals.sumOf { it.weight }.coerceIn(0, 100)
        return Result(score, levelOf(score), summarizeUrl(levelOf(score), domain, signals), signals, listOf(normalized))
    }

    fun looksLikeUrl(input: String): Boolean {
        val trimmed = input.trim()
        return !trimmed.contains(" ") &&
            (trimmed.startsWith("http://") || trimmed.startsWith("https://") || Regex("""^[\w-]+(\.[\w-]+){1,}(/.*)?$""").matches(trimmed))
    }

    fun levelOf(score: Int) = when {
        score >= 70 -> "dangerous"
        score >= 35 -> "suspicious"
        else -> "safe"
    }

    private fun summarizeText(level: String, signals: List<Signal>): String {
        val head = when (level) {
            "dangerous" -> "Ce message présente les caractéristiques d'une arnaque. Ne réponds pas, ne clique pas, ne paie rien et ne communique aucun code."
            "suspicious" -> "Ce message contient des signaux suspects. Vérifie l'expéditeur par un canal officiel avant d'agir."
            else -> "Aucun signal d'arnaque marquant détecté. Reste prudent si on te demande de l'argent ou un code."
        }
        val top = signals.filter { it.weight > 0 }.sortedByDescending { it.weight }.take(2)
        return if (top.isEmpty()) head else head + " " + top.joinToString(" ") { it.label }
    }

    private fun summarizeUrl(level: String, domain: String, signals: List<Signal>): String {
        val head = when (level) {
            "dangerous" -> "Risque élevé : n'ouvre pas $domain et n'y saisis aucune information."
            "suspicious" -> "Signaux suspects sur $domain : vérifie la provenance avant d'ouvrir."
            else -> "Aucun signal de danger significatif sur $domain."
        }
        val top = signals.filter { it.weight >= 15 }.sortedByDescending { it.weight }.take(2)
        return if (top.isEmpty()) head else head + " " + top.joinToString(" ") { it.label }
    }

    private fun registrableDomain(host: String): String {
        val parts = host.trim('.').split(".")
        return if (parts.size <= 2) parts.joinToString(".") else parts.takeLast(2).joinToString(".")
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(previous[j + 1] + 1, current[j] + 1, previous[j] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }
}
