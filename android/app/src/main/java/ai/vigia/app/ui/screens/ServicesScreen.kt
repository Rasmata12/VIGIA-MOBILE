package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.BentoActionCard
import ai.vigia.app.ui.components.SectionHeader
import ai.vigia.app.ui.components.SecurityLessonCard
import ai.vigia.app.ui.theme.*

/** Hub des Services VIGIA AI :
 * Regroupe tous les modules spécialisés et l'Académie de conseils & leçons,
 * avec une hiérarchie visuelle riche sans aucun bloc blanc générique. */
@Composable
fun ServicesScreen(onNavigate: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(top = 22.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column {
            Text(
                "Hub des Services",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = VigiaTextPrimary,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tous les outils spécialisés de protection cyber et anti-fraude VIGIA.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }

        // ------------------------------------------------------ PAIEMENTS & ARGENT
        Column {
            SectionHeader(
                title = "Finances & Transactions",
                subtitle = "Sécurisez vos transferts d'argent avant tout envoi"
            )
            Spacer(Modifier.height(10.dp))
            BentoActionCard(
                title = "Before Pay™ — Anti-Arnaque",
                subtitle = "Auditez un transfert Mobile Money (Wave, Orange, MoMo) ou virement bancaire avant de valider",
                icon = Icons.Rounded.AccountBalanceWallet,
                color = RiskDanger,
                tag = "MOBILE MONEY",
                featured = true,
                modifier = Modifier.fillMaxWidth()
            ) { onNavigate("before_pay") }
        }

        // ------------------------------------------------------ OPPORTUNITÉS & ANNONCES
        Column {
            SectionHeader(
                title = "Opportunités & Achats",
                subtitle = "Ne tombez plus dans les pièges d'embauche ou de fausses annonces"
            )
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 390.dp
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BentoActionCard(
                            title = "Offres d'Emploi",
                            subtitle = "Faux recruteurs, frais de dossier frauduleux",
                            icon = Icons.Rounded.WorkspacePremium,
                            color = Color(0xFFD97706),
                            tag = "RECRUTEMENT",
                            modifier = Modifier.fillMaxWidth()
                        ) { onNavigate("job_offer") }
                        BentoActionCard(
                            title = "Petites Annonces",
                            subtitle = "Immobilier, autos, acomptes interdits",
                            icon = Icons.Rounded.Storefront,
                            color = VigiaViolet,
                            tag = "MARKETPLACE",
                            modifier = Modifier.fillMaxWidth()
                        ) { onNavigate("listing") }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        BentoActionCard(
                            title = "Offres d'Emploi",
                            subtitle = "Faux recruteurs, frais de dossier frauduleux",
                            icon = Icons.Rounded.WorkspacePremium,
                            color = Color(0xFFD97706),
                            tag = "RECRUTEMENT",
                            modifier = Modifier.weight(1f)
                        ) { onNavigate("job_offer") }
                        BentoActionCard(
                            title = "Petites Annonces",
                            subtitle = "Immobilier, autos, acomptes interdits",
                            icon = Icons.Rounded.Storefront,
                            color = VigiaViolet,
                            tag = "MARKETPLACE",
                            modifier = Modifier.weight(1f)
                        ) { onNavigate("listing") }
                    }
                }
            }
        }

        // ------------------------------------------------------ ACADÉMIE VIGIA (CONSEILS & LEÇONS)
        Column {
            SectionHeader(
                title = "Académie Cyber — Conseils & Leçons",
                subtitle = "Fiches réflexes interactives pour développer votre immunité numérique"
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecurityLessonCard(
                    title = "Comment démasquer une fausse offre d'emploi",
                    category = "LEÇON EMPLOI",
                    takeaway = "Un recruteur légitime ne vous demandera JAMAIS d'argent pour des 'frais de badge', d'uniforme ou de dossier médical avant embauche.",
                    tips = listOf(
                        "Méfiez-vous des adresses de contact gratuites : recrutement-total@gmail.com n'est JAMAIS une adresse officielle.",
                        "Salaires exorbitants pour un travail simple à domicile : signal d'alarme immédiat.",
                        "Recherchez le nom de l'entreprise sur Google accompagné du mot 'arnaque' avant de postuler."
                    ),
                    color = Color(0xFFD97706)
                )

                SecurityLessonCard(
                    title = "Achat en ligne : les règles pour ne pas perdre son acompte",
                    category = "LEÇON MARKETPLACE",
                    takeaway = "N'envoyez JAMAIS d'acompte pour 'réserver' un véhicule, un logement ou un smartphone sans l'avoir vu en personne.",
                    tips = listOf(
                        "Exigez toujours une rencontre physique dans un lieu public avant tout transfert.",
                        "Faux avis de livraison ou faux transporteur qui demande des 'frais d'assurance' remboursables : arnaque classique.",
                        "Utilisez le paiement sécurisé intégré aux plateformes reconnues plutôt que des transferts directs non protégés."
                    ),
                    color = VigiaViolet
                )
            }
        }

        // ------------------------------------------------------ SÉCURITÉ CITOYENNE & RADAR
        Column {
            SectionHeader(
                title = "Protection Collective",
                subtitle = "Participez au réseau d'alerte et surveillez les attaques coordonnées"
            )
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 390.dp
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BentoActionCard(
                            title = "Communauté",
                            subtitle = "Signaler un escroc ou consulter les alertes",
                            icon = Icons.Rounded.Diversity3,
                            color = RiskSafe,
                            tag = "CITOYEN",
                            modifier = Modifier.fillMaxWidth()
                        ) { onNavigate("community") }
                        BentoActionCard(
                            title = "Radar Moment",
                            subtitle = "Vagues de phishing en temps réel",
                            icon = Icons.Rounded.Radar,
                            color = VigiaPrimary,
                            tag = "LIVE 6H",
                            modifier = Modifier.fillMaxWidth()
                        ) { onNavigate("moment_shield") }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        BentoActionCard(
                            title = "Communauté",
                            subtitle = "Signaler un escroc ou consulter les alertes",
                            icon = Icons.Rounded.Diversity3,
                            color = RiskSafe,
                            tag = "CITOYEN",
                            modifier = Modifier.weight(1f)
                        ) { onNavigate("community") }
                        BentoActionCard(
                            title = "Radar Moment",
                            subtitle = "Vagues de phishing en temps réel",
                            icon = Icons.Rounded.Radar,
                            color = VigiaPrimary,
                            tag = "LIVE 6H",
                            modifier = Modifier.weight(1f)
                        ) { onNavigate("moment_shield") }
                    }
                }
            }
        }

        // ------------------------------------------------------ HISTORIQUE FORENSIQUE
        Column {
            SectionHeader(
                title = "Archives & Rapports",
                subtitle = "Retrouvez l'historique complet de toutes vos vérifications"
            )
            Spacer(Modifier.height(10.dp))
            BentoActionCard(
                title = "Journal d'Analyses Forensiques",
                subtitle = "Consultez tous vos rapports passés, scores de risque et preuves techniques archivées",
                icon = Icons.Rounded.History,
                color = VigiaPrimaryBright,
                tag = "ARCHIVES",
                modifier = Modifier.fillMaxWidth()
            ) { onNavigate("history") }
        }
    }
}
