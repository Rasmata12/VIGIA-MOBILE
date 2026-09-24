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
    "objet" to "Tech & Objets",
    "service" to "Prestations",
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

    val listingColor = Color(0xFF0891B2) // Cyan-600

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // En-tête de navigation
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubtleBackButton(onBack = onBack, label = "Retour")
            Spacer(Modifier.weight(1f))
            InfoChip("Petites Annonces", listingColor)
        }

        // Titre & Sous-titre
        Column {
            Text(
                "Audit Petites Annonces",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = VigiaTextPrimary,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Détectez les faux vendeurs et les acomptes frauduleux avant tout achat.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        // Conseil de vérification avant achat
        HeroSurface(orbColors = listOf(listingColor, VigiaPrimary), cornerRadius = 20.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(icon = Icons.Rounded.Storefront, tint = listingColor, size = 40.dp, iconSize = 20.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Remise en main propre",
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily,
                        color = listingColor,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Ne versez aucun acompte avant d'avoir vu et testé le bien en direct.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Sélection de catégorie
        GlassCard(
            backgroundBrush = luxuryCardGradient(listingColor),
            borderBrush = luxuryBorderGradient(listingColor),
            cornerRadius = 24.dp
        ) {
            Text(
                "Catégorie de l'annonce",
                fontWeight = FontWeight.Bold,
                fontFamily = PoppinsFontFamily,
                color = VigiaTextPrimary,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORIES.take(3).forEach { (key, label) ->
                    val selected = category == key
                    FilterChip(
                        selected = selected,
                        onClick = { category = key },
                        label = {
                            Text(
                                label,
                                fontFamily = PoppinsFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = listingColor,
                            selectedLabelColor = Color.White,
                            containerColor = VigiaSurfaceHigh,
                            labelColor = VigiaTextSecondary
                        )
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORIES.drop(3).forEach { (key, label) ->
                    val selected = category == key
                    FilterChip(
                        selected = selected,
                        onClick = { category = key },
                        label = {
                            Text(
                                label,
                                fontFamily = PoppinsFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = listingColor,
                            selectedLabelColor = Color.White,
                            containerColor = VigiaSurfaceHigh,
                            labelColor = VigiaTextSecondary
                        )
                    )
                }
            }
        }

        // Formulaire d'audit
        GlassCard(
            backgroundBrush = luxuryCardGradient(listingColor),
            borderBrush = luxuryBorderGradient(listingColor),
            cornerRadius = 24.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Rounded.Sell, tint = listingColor, size = 36.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Détails de l'offre",
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextPrimary,
                        fontSize = 15.sp
                    )
                }
                TextButton(onClick = {
                    category = "vehicule"
                    priceAsked = "2 500 000 FCFA (Toyota RAV4 2021)"
                    sellerContact = "+225 07 99 88 77 (WhatsApp uniquement)"
                    depositRequested = "150 000 FCFA (Acompte de réservation)"
                    canVisit = false
                    content = "Véhicule propre première main, prix sacrifié pour départ urgent à l'étranger. Impossible de visiter car la voiture est au dépôt douanier. Pour réserver avant les autres acquéreurs, versez 150 000 FCFA remboursables par Wave."
                }) {
                    Text(
                        "Exemple suspect",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = listingColor
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            VigiaField(
                value = priceAsked,
                onValueChange = { priceAsked = it },
                label = "Prix demandé (ex: 2 500 000 FCFA)",
                supporting = "Les prix anormalement bas cachent souvent une escroquerie"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = sellerContact,
                onValueChange = { sellerContact = it },
                label = "Contact du vendeur (Téléphone, WhatsApp...)",
                supporting = "Attention aux vendeurs refusant les appels vocaux normaux"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = depositRequested,
                onValueChange = { depositRequested = it },
                label = "Acompte ou avance exigée (le cas échéant)",
                supporting = "Frais de réservation, gardiennage ou livraison exigés avant visite"
            )

            Spacer(Modifier.height(14.dp))

            Text(
                "Le vendeur accepte-t-il une visite physique sans avance préalable ?",
                fontFamily = PoppinsFontFamily,
                fontSize = 12.5.sp,
                color = VigiaTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = canVisit == true,
                    onClick = { canVisit = true },
                    label = {
                        Text("Oui, visite libre", fontFamily = PoppinsFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RiskSafe,
                        selectedLabelColor = Color.White,
                        containerColor = VigiaSurfaceHigh,
                        labelColor = VigiaTextSecondary
                    )
                )
                FilterChip(
                    selected = canVisit == false,
                    onClick = { canVisit = false },
                    label = {
                        Text("Non / Exige un acompte", fontFamily = PoppinsFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RiskDanger,
                        selectedLabelColor = Color.White,
                        containerColor = VigiaSurfaceHigh,
                        labelColor = VigiaTextSecondary
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            VigiaField(
                value = content,
                onValueChange = { content = it },
                label = "Texte complet de l'annonce ou discussion",
                minLines = 4,
                singleLine = false,
                supporting = "Copiez la description ou les échanges sur WhatsApp/Facebook"
            )

            Spacer(Modifier.height(18.dp))

            GradientButton(
                text = "Lancer l'audit de l'annonce",
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
            SectionHeader("Verdict de l'Audit")

            DecisionBanner(decision = res.decision, headline = res.headline)

            if (res.redFlags.isNotEmpty()) {
                GlassCard(
                    backgroundBrush = luxuryCardGradient(RiskDanger),
                    borderBrush = luxuryBorderGradient(RiskDanger),
                    cornerRadius = 24.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Rounded.Report, tint = RiskDanger, size = 36.dp, iconSize = 18.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Signaux d'Alerte Majeurs",
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PoppinsFontFamily,
                            color = RiskDanger,
                            fontSize = 14.5.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    res.redFlags.forEach { flag ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = RiskDanger, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                flag,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextPrimary,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }

            res.assessment?.let { assessment ->
                GlassCard(
                    backgroundBrush = luxuryCardGradient(VigiaPrimary),
                    borderBrush = luxuryBorderGradient(VigiaPrimary),
                    cornerRadius = 24.dp
                ) {
                    Text(
                        "Indice de Fiabilité Globale",
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        RiskGauge(score = assessment.score, level = assessment.level, size = 160.dp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        assessment.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }

                if (assessment.scamDna.isNotEmpty()) {
                    SectionHeader("Marqueurs Détectés (Scam DNA)")
                    assessment.scamDna.forEach { trait ->
                        ScamDnaCard(
                            category = trait.category,
                            label = trait.label,
                            strength = trait.strength,
                            evidence = trait.evidence
                        )
                    }
                }
            }

            if (res.checklist.isNotEmpty()) {
                GlassCard(
                    backgroundBrush = luxuryCardGradient(VigiaCyan),
                    borderBrush = luxuryBorderGradient(VigiaCyan),
                    cornerRadius = 24.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Rounded.FactCheck, tint = VigiaCyan, size = 34.dp, iconSize = 17.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Checklist de Sécurité avant Achat",
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily,
                            color = VigiaCyan,
                            fontSize = 14.5.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    res.checklist.forEachIndexed { idx, point ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Text(
                                "${idx + 1}.",
                                color = VigiaCyan,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                point,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextPrimary,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
