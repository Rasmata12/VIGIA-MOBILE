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
import ai.vigia.app.ui.theme.*

/** Hub des Services VIGIA AI :
 * Regroupe les modules spécialisés et donne accès à une académie de leçons dédiée,
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

        // ------------------------------------------------------ ACADÉMIE VIGIA (ESPACE DÉDIÉ)
        Column {
            SectionHeader(
                title = "Apprendre",
                subtitle = "Des leçons pratiques, réunies dans un espace dédié"
            )
            Spacer(Modifier.height(10.dp))
            BentoActionCard(
                title = "Leçons de cybersécurité",
                subtitle = "Apprenez à repérer les faux liens, offres, annonces et demandes de paiement.",
                icon = Icons.Rounded.School,
                color = VigiaPrimaryBright,
                tag = "ACADÉMIE",
                modifier = Modifier.fillMaxWidth()
            ) { onNavigate("lessons") }
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
