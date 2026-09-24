package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.JobOfferViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun JobOfferScreen(
    viewModel: JobOfferViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var content by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var salaryPromised by remember { mutableStateOf("") }
    var feeRequested by remember { mutableStateOf("") }
    var showOptionalDetails by remember { mutableStateOf(false) }
    var showAssessmentDetails by remember { mutableStateOf(false) }
    var showChecklist by remember { mutableStateOf(false) }

    val amberColor = Color(0xFFD97706)

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubtleBackButton(onBack = onBack, label = "Retour")
            Spacer(Modifier.weight(1f))
            InfoChip("Audit Recrutement", amberColor)
        }

        Column {
            Text(
                "Audit d'Offre d'Emploi",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = VigiaTextPrimary,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Détectez les faux recruteurs, les frais illégaux exigés avant embauche et les salaires miroirs.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }

        // HÉROS — Règle universelle de recrutement
        HeroSurface(orbColors = listOf(amberColor, VigiaSecondary), cornerRadius = 24.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconBadge(icon = Icons.Rounded.WorkspacePremium, tint = amberColor, size = 44.dp, iconSize = 22.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Points de vigilance recrutement",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PoppinsFontFamily,
                        color = amberColor,
                        fontSize = 14.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Des frais exigés pour obtenir un poste sont un signal majeur. Les éventuels frais de formation externes doivent être vérifiés indépendamment et ne doivent pas être présentés comme une condition cachée d'embauche.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Formulaire d'audit
        GlassCard(
            backgroundBrush = luxuryCardGradient(amberColor),
            borderBrush = luxuryBorderGradient(amberColor),
            cornerRadius = 24.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Rounded.Description, tint = amberColor, size = 36.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Détails de l'annonce",
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextPrimary,
                        fontSize = 15.sp
                    )
                }
                TextButton(onClick = {
                    companyName = "ONG Internationale Afrique Secours"
                    contactEmail = "recrutement.ong.afrique@gmail.com"
                    salaryPromised = "850 000 FCFA / mois"
                    feeRequested = "15 000 FCFA (Frais d'ouverture de dossier)"
                    content = "Urgent : Nous recrutons 10 assistants administratifs à distance. Salaire 850 000 FCFA. Envoyez 15 000 FCFA pour valider votre dossier."
                }) {
                    Text("Exemple suspect", fontFamily = PoppinsFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = amberColor)
                }
            }

            Spacer(Modifier.height(14.dp))

            VigiaField(
                value = content,
                onValueChange = { content = it },
                label = "Texte intégral de l'offre (recommandé)",
                minLines = 5,
                singleLine = false,
                supporting = "Collez le message, l'e-mail ou le texte visible sur la capture. C'est l'entrée principale de l'audit."
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOptionalDetails = !showOptionalDetails },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (showOptionalDetails) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = amberColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Ajouter des précisions (facultatif)",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = VigiaTextPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        "Entreprise, contact, salaire ou frais demandés. Pas besoin de tout remplir.",
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            AnimatedVisibility(visible = showOptionalDetails) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    VigiaField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = "Entreprise / organisme (facultatif)",
                        supporting = "Permet de vérifier le contexte du recruteur."
                    )
                    Spacer(Modifier.height(10.dp))
                    VigiaField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
                        label = "E-mail ou contact (facultatif)",
                        supporting = "Un Gmail/WhatsApp n'est pas une preuve d'arnaque à lui seul."
                    )
                    Spacer(Modifier.height(10.dp))
                    VigiaField(
                        value = salaryPromised,
                        onValueChange = { salaryPromised = it },
                        label = "Rémunération promise (facultatif)"
                    )
                    Spacer(Modifier.height(10.dp))
                    VigiaField(
                        value = feeRequested,
                        onValueChange = { feeRequested = it },
                        label = "Frais demandés avant embauche (facultatif)",
                        supporting = "Indiquez tout paiement exigé pour obtenir le poste ou commencer la formation."
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            GradientButton(
                text = "Lancer l'audit de l'offre",
                onClick = {
                    viewModel.verify(
                        content = content,
                        companyName = companyName,
                        contactEmail = contactEmail,
                        salaryPromised = salaryPromised,
                        feeRequested = feeRequested
                    )
                },
                loading = state.loading,
                enabled = content.isNotBlank(),
                icon = Icons.Rounded.VerifiedUser,
                gradient = androidx.compose.ui.graphics.SolidColor(amberColor),
                modifier = Modifier.fillMaxWidth()
            )
        }

        state.error?.let {
            ErrorBanner(it, onRetry = {
                viewModel.verify(content, companyName, contactEmail, salaryPromised, feeRequested)
            })
        }

        state.result?.let { res ->
            SectionHeader("Verdict d'Authenticité")

            DecisionBanner(
                decision = res.decision,
                headline = res.headline
            )

            if (res.redFlags.isNotEmpty()) {
                GlassCard(
                    backgroundBrush = luxuryCardGradient(RiskDanger),
                    borderBrush = luxuryBorderGradient(RiskDanger),
                    cornerRadius = 22.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Rounded.ReportProblem, tint = RiskDanger, size = 36.dp, iconSize = 18.dp)
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
                    backgroundColor = Color.White,
                    borderColor = VigiaBorder,
                    cornerRadius = 22.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showAssessmentDetails = !showAssessmentDetails },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(icon = Icons.Rounded.ManageSearch, tint = amberColor, size = 36.dp, iconSize = 18.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Pourquoi ce verdict ?", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                            Text("Score ${assessment.score}/100 • ouvre les éléments de preuve", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 11.5.sp)
                        }
                        Icon(if (showAssessmentDetails) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = amberColor)
                    }
                    AnimatedVisibility(showAssessmentDetails) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                RiskGauge(score = assessment.score, level = assessment.level, size = 150.dp)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(assessment.summary, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.5.sp, lineHeight = 18.sp)
                            if (assessment.scamDna.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                assessment.scamDna.forEach { trait ->
                                    ScamDnaCard(category = trait.category, label = trait.label, strength = trait.strength, evidence = trait.evidence)
                                }
                            }
                        }
                    }
                }
            }

            if (res.checklist.isNotEmpty()) {
                GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder, cornerRadius = 22.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showChecklist = !showChecklist },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(icon = Icons.Rounded.FactCheck, tint = VigiaPrimary, size = 34.dp, iconSize = 17.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("À vérifier avant de répondre", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                            Text("${res.checklist.size} conseils pratiques", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 11.5.sp)
                        }
                        Icon(if (showChecklist) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = VigiaPrimary)
                    }
                    AnimatedVisibility(showChecklist) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            res.checklist.forEachIndexed { idx, point ->
                                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                    Text("${idx + 1}.", color = VigiaPrimary, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(point, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 12.5.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
