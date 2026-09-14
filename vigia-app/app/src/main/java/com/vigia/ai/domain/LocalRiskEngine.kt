package com.vigia.ai.domain

import java.util.Locale

enum class RiskLevel(val label: String) {
    FAIBLE("FAIBLE"),
    MODERE("MODÉRÉ"),
    ELEVE("ÉLEVÉ"),
    CRITIQUE("CRITIQUE")
}

data class RiskResult(
    val score: Int,
    val level: RiskLevel,
    val signals: List<String>,
    val recommendation: String,
    val targetedBrand: String? = null
)

object LocalRiskEngine {
    private val URGENCY_REGEX = Regex("(?i)\\b(urgent|immédiat(ement)?|suspendu|bloqu(é|er|era)|alerte|expiration|dernier délai|sous 24h|sous 2h)\\b")
    private val FINANCIAL_REGEX = Regex("(?i)\\b(fcfa|cfa|payer|paiement|virement|transfert|solde|mobile|orange|moov|mtn|wave|ecobank|coris|frais|dépôt|recharge)\\b")
    private val CREDENTIAL_REGEX = Regex("(?i)\\b(mot de passe|code secret|otp|pin|identifiant|carte bancaire|cvv|renvoyer le code|confirmer code)\\b")
    private val LINK_REGEX = Regex("(?i)(https?://[^\\s]+|www\\.[^\\s]+)")
    private val SUSPICIOUS_TLD_REGEX = Regex("(?i)\\.(top|xyz|tk|ml|ga|cf|gq|click|buzz|rest|icu|work|cam|sbs|monster)\\b")
    private val IP_HOST_REGEX = Regex("(https?://)?(\\d{1,3}\\.){3}\\d{1,3}")

    private val BRAND_PATTERNS = listOf(
        Pair("Orange Money", Regex("(?i)\\borange(-?money)?\\b")),
        Pair("Moov Money", Regex("(?i)\\bmoov(-?money)?\\b")),
        Pair("MTN MoMo", Regex("(?i)\\bmtn(-?momo)?\\b")),
        Pair("Wave Mobile Money", Regex("(?i)\\bwave\\b")),
        Pair("Ecobank", Regex("(?i)\\becobank\\b")),
        Pair("Coris Bank", Regex("(?i)\\bcoris\\b"))
    )

    fun analyzeText(text: String): RiskResult {
        var currentScore = 0
        val detectedSignals = mutableListOf<String>()
        val lowerText = text.lowercase(Locale.getDefault())

        // 1. Détection de pression ou urgence
        if (URGENCY_REGEX.containsMatchIn(lowerText)) {
            currentScore += 25
            detectedSignals.add("Urgence artificielle ou pression psychologique")
        }

        // 2. Détection de demande monétaire ou virement
        if (FINANCIAL_REGEX.containsMatchIn(lowerText)) {
            currentScore += 25
            detectedSignals.add("Contexte financier ou Mobile Money ciblé")
        }

        // 3. Détection de vol d'identifiants (OTP / PIN)
        if (CREDENTIAL_REGEX.containsMatchIn(lowerText)) {
            currentScore += 35
            detectedSignals.add("Demande d'informations ultra-sensibles (PIN/OTP)")
        }

        // 4. Détection de lien URL
        if (LINK_REGEX.containsMatchIn(lowerText)) {
            currentScore += 15
            detectedSignals.add("Lien hypertexte détecté dans le message")

            if (SUSPICIOUS_TLD_REGEX.containsMatchIn(lowerText)) {
                currentScore += 25
                detectedSignals.add("Domaine avec extension à fort risque de fraude (.top, .xyz, etc.)")
            }
            if (IP_HOST_REGEX.containsMatchIn(lowerText)) {
                currentScore += 35
                detectedSignals.add("Lien utilisant directement une adresse IP au lieu d'un domaine")
            }
        }

        // 5. Détection d'usurpation de marque
        var identifiedBrand: String? = null
        for ((brandName, regex) in BRAND_PATTERNS) {
            if (regex.containsMatchIn(lowerText)) {
                identifiedBrand = brandName
                detectedSignals.add("Mention de l'entité de confiance : $brandName")
                break
            }
        }

        val finalScore = currentScore.coerceAtMost(100)
        val level = when {
            finalScore < 30 -> RiskLevel.FAIBLE
            finalScore < 60 -> RiskLevel.MODERE
            finalScore < 80 -> RiskLevel.ELEVE
            else -> RiskLevel.CRITIQUE
        }

        val recommendation = when (level) {
            RiskLevel.CRITIQUE, RiskLevel.ELEVE ->
                "ALERTE : Ne transmettez aucun code, PIN ou virement. Les banques et opérateurs officiels ne demandent JAMAIS vos codes secrets."
            RiskLevel.MODERE ->
                "VIGILANCE : Vérifiez scrupuleusement l'expéditeur et ne cliquez sur aucun lien non sollicité."
            RiskLevel.FAIBLE ->
                "Aucun indicateur de fraude direct décelé."
        }

        return RiskResult(finalScore, level, detectedSignals, recommendation, identifiedBrand)
    }
}
