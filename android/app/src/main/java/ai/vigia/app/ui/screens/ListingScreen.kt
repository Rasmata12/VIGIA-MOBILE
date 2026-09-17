package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.ListingViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val CATEGORIES = listOf(
    "immobilier" to "Immobilier",
    "vehicule" to "Véhicule",
    "objet" to "Objet",
    "service" to "Service",
    "autre" to "Autre"
)

@Composable
fun ListingScreen(
    viewModel: ListingViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("autre") }
    var priceAsked by remember { mutableStateOf("") }
    var sellerContact by remember { mutableStateOf("") }
    var depositRequested by remember { mutableStateOf("") }
    var canVisit by remember { mutableStateOf<Boolean?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour", tint = VigiaPrimary)
            }
            Text("Retour", fontFamily = PoppinsFontFamily, color = VigiaPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            InfoChip("Petites annonces", VigiaCyan)
        }

        Column {
            Text("Vérifier une annonce", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Immobilier, véhicules, objets, services : détecte les prix trop beaux, les acomptes exigés avant rencontre.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        GlassCard {
            Text("Catégorie", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORIES.forEach { (key, label) ->
                    val selected = category == key
                    FilterChip(
                        selected = selected,
                        onClick = { category = key },
                        label = { Text(label, fontFamily = PoppinsFontFamily, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VigiaPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = VigiaSurfaceHigh,
                            labelColor = VigiaTextSecondary
                        )
                    )
                }
            }
        }

        GlassCard {
            Text("Détails de l'annonce", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))

            VigiaField(
                value = priceAsked,
                onValueChange = { priceAsked = it },
                label = "Prix demandé"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = sellerContact,
                onValueChange = { sellerContact = it },
                label = "Contact du vendeur (téléphone, WhatsApp...)"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = depositRequested,
                onValueChange = { depositRequested = it },
                label = "Acompte ou arrhes demandés (le cas échéant)",
                supporting = "Montant exigé avant toute visite ou rencontre"
            )

            Spacer(Modifier.height(12.dp))

            Text("Peux-tu visiter / voir l'objet en personne avant de payer ?", fontFamily = PoppinsFontFamily, fontSize = 13.sp, color = VigiaTextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = canVisit == true,
                    onClick = { canVisit = true },
                    label = { Text("Oui", fontFamily = PoppinsFontFamily, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RiskSafe, selectedLabelColor = Color.White)
                )
                FilterChip(
                    selected = canVisit == false,
                    onClick = { canVisit = false },
                    label = { Text("Non", fontFamily = PoppinsFontFamily, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RiskDanger, selectedLabelColor = Color.White)
                )
            }

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = content,
                onValueChange = { content = it },
                label = "Texte complet de l'annonce",
                minLines = 4,
                singleLine = false,
                supporting = "Colle la description ou le message échangé avec le vendeur"
            )

            Spacer(Modifier.height(16.dp))

            GradientButton(
                text = "Analyser cette annonce",
                onClick = { viewModel.verify(content, category, priceAsked, sellerContact, depositRequested, canVisit) },
                loading = state.loading,
                enabled = content.isNotBlank(),
                icon = Icons.Rounded.VerifiedUser,
                modifier = Modifier.fillMaxWidth()
            )
        }

        state.error?.let {
            ErrorBanner(it, onRetry = { viewModel.verify(content, category, priceAsked, sellerContact, depositRequested, canVisit) })
        }

        state.result?.let { res ->
            SectionHeader("Verdict")

            DecisionBanner(decision = res.decision, headline = res.headline)

            if (res.redFlags.isNotEmpty()) {
                GlassCard(borderColor = RiskDangerBorder, backgroundColor = RiskDangerBg) {
                    Text("Signaux d'alerte détectés", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskDanger, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    res.redFlags.forEach { flag ->
                        Text("• $flag", style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
                    }
                }
            }

            res.assessment?.let { assessment ->
                GlassCard {
                    Text("Analyse détaillée", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        RiskGauge(score = assessment.score, level = assessment.level, size = 160.dp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(assessment.summary, style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary)
                }

                if (assessment.scamDna.isNotEmpty()) {
                    SectionHeader("Marqueurs détectés (Scam DNA)")
                    assessment.scamDna.forEach { trait ->
                        ScamDnaCard(category = trait.category, label = trait.label, strength = trait.strength, evidence = trait.evidence)
                    }
                }
            }

            if (res.checklist.isNotEmpty()) {
                GlassCard(borderColor = Color(0xFFDBEAFE), backgroundColor = Color(0xFFEFF6FF)) {
                    Text("Checklist avant d'acheter :", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaPrimary, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    res.checklist.forEachIndexed { idx, point ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Text("${idx + 1}.", color = VigiaPrimary, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(point, style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
                        }
                    }
                }
            }
        }
    }
}
