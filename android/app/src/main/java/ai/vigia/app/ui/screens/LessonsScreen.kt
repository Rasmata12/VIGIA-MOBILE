package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.SecurityLessonCard
import ai.vigia.app.ui.components.SubtleBackButton
import ai.vigia.app.ui.theme.BackgroundGradient
import ai.vigia.app.ui.theme.PoppinsFontFamily
import ai.vigia.app.ui.theme.RiskDanger
import ai.vigia.app.ui.theme.VigiaPrimary
import ai.vigia.app.ui.theme.VigiaPrimaryBright
import ai.vigia.app.ui.theme.VigiaTextPrimary
import ai.vigia.app.ui.theme.VigiaTextSecondary
import ai.vigia.app.ui.theme.VigiaViolet

@Composable
fun LessonsScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SubtleBackButton(onBack = onBack, label = "Services")
        Text(
            "Académie VIGIA",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 25.sp,
            color = VigiaTextPrimary
        )
        Text(
            "Leçons courtes pour reconnaître les pièges avant d’agir. Touchez une fiche pour afficher ses conseils.",
            fontFamily = PoppinsFontFamily,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = VigiaTextSecondary
        )
        Spacer(Modifier.height(2.dp))

        SecurityLessonCard(
            title = "Protéger ses codes et paiements",
            category = "COMPTES ET PAIEMENTS",
            takeaway = "Ne communiquez jamais votre code PIN, votre mot de passe ni un code reçu par SMS. Un agent bancaire ou Mobile Money n’a pas besoin de ces secrets pour vous aider.",
            tips = listOf(
                "Un code à usage unique sert à valider une action : ne le dictez à personne, même si l’appel semble officiel.",
                "En cas d’appel pressant, raccrochez puis contactez le service depuis son application ou son numéro officiel.",
                "Avant un transfert, vérifiez le nom affiché du bénéficiaire et le montant sur l’écran de confirmation."
            ),
            color = RiskDanger
        )

        SecurityLessonCard(
            title = "Repérer un faux lien",
            category = "LIENS ET MESSAGES",
            takeaway = "Un cadenas HTTPS ne prouve pas qu’un site est légitime. Lisez le nom de domaine complet et méfiez-vous des liens inattendus, raccourcis ou qui imitent une marque connue.",
            tips = listOf(
                "Ouvrez vous-même le site officiel depuis vos favoris au lieu de suivre un lien reçu par message.",
                "Vérifiez chaque lettre du domaine : une adresse qui ressemble à une marque peut appartenir à un fraudeur.",
                "Ne saisissez aucun identifiant si le message vous presse ou menace de bloquer votre compte."
            ),
            color = VigiaPrimary
        )

        SecurityLessonCard(
            title = "Vérifier une offre d’emploi",
            category = "RECRUTEMENT",
            takeaway = "Un employeur sérieux ne vous demande pas de payer des frais de dossier, de badge, de formation ou de matériel pour obtenir un poste.",
            tips = listOf(
                "Vérifiez l’offre sur le site officiel de l’entreprise et contactez-la avec des coordonnées trouvées indépendamment.",
                "Méfiez-vous des salaires très élevés pour peu de travail et des recrutements garantis sans entretien.",
                "Ne transmettez pas vos pièces d’identité ni vos coordonnées bancaires avant d’avoir vérifié l’employeur."
            ),
            color = Color(0xFFD97706)
        )

        SecurityLessonCard(
            title = "Acheter en ligne sans perdre son acompte",
            category = "PETITES ANNONCES",
            takeaway = "Ne versez pas d’acompte pour réserver un téléphone, un véhicule ou un logement que vous n’avez pas pu vérifier.",
            tips = listOf(
                "Rencontrez le vendeur dans un lieu sûr et examinez le bien avant le paiement.",
                "Refusez les frais de livraison, d’assurance ou de déblocage ajoutés à la dernière minute.",
                "Utilisez le paiement protégé de la plateforme lorsqu’il existe; un transfert direct est difficile à récupérer."
            ),
            color = VigiaViolet
        )

        SecurityLessonCard(
            title = "Éviter les faux frais de livraison",
            category = "SMS ET LIVRAISON",
            takeaway = "Un SMS qui réclame un petit paiement pour débloquer un colis peut servir à voler tes coordonnées ou ton code de paiement.",
            tips = listOf(
                "Ne clique pas sur le lien du SMS : ouvre toi-même l'application ou le site officiel du livreur.",
                "Vérifie le numéro de suivi auprès de la boutique où tu as passé commande.",
                "Ne donne jamais ton code reçu par SMS pour recevoir un colis."
            ),
            color = Color(0xFF0E9F6E)
        )

        SecurityLessonCard(
            title = "Garder son compte WhatsApp",
            category = "MESSAGERIE",
            takeaway = "Le code reçu par SMS permet de prendre le contrôle de ton compte. Même un ami ou un agent qui le demande ne doit pas le recevoir.",
            tips = listOf(
                "Ne partage jamais le code de connexion reçu par SMS.",
                "Active la vérification en deux étapes dans les réglages de WhatsApp.",
                "Si ton compte est bloqué, récupère-le depuis l'application officielle et préviens tes proches par un autre moyen."
            ),
            color = VigiaPrimaryBright
        )
    }
}
