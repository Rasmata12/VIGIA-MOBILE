package ai.vigia.app

import ai.vigia.app.engine.LocalAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests unitaires du moteur local (execution: ./gradlew test). */
class LocalAnalyzerTest {

    @Test
    fun phishingMessageIsFlaggedDangerous() {
        val message = "URGENT: votre compte Orange Money sera suspendu dans 24h. " +
            "Confirmez votre code PIN ici http://orange-money.verif.tk/login sinon blocage definitif!!!!"
        val result = LocalAnalyzer.analyzeText(message)
        assertEquals("dangerous", result.level)
        assertTrue(result.signals.any { it.code == "identifiants" })
        assertTrue(result.signals.any { it.code == "urgence" })
    }

    @Test
    fun normalMessageIsSafe() {
        val result = LocalAnalyzer.analyzeText("Bonjour, je confirme le rendez-vous de demain a 15h au bureau.")
        assertEquals("safe", result.level)
    }

    @Test
    fun typosquattedDomainIsDangerous() {
        val result = LocalAnalyzer.analyzeUrl("http://paypa1.com/login/verify")
        assertEquals("dangerous", result.level)
        assertTrue(result.signals.any { it.code == "typosquat" })
    }

    @Test
    fun legitimateDomainIsSafe() {
        val result = LocalAnalyzer.analyzeUrl("https://www.wikipedia.org/wiki/Test")
        assertEquals("safe", result.level)
    }

    @Test
    fun invalidInputIsNotFaked() {
        val result = LocalAnalyzer.analyzeUrl("ceci n est pas une url")
        assertEquals("invalid", result.level)
    }

    @Test
    fun urlDetectionWorks() {
        assertTrue(LocalAnalyzer.looksLikeUrl("https://exemple.com/a"))
        assertTrue(LocalAnalyzer.looksLikeUrl("exemple.com"))
        assertTrue(!LocalAnalyzer.looksLikeUrl("bonjour comment ca va"))
    }
}
