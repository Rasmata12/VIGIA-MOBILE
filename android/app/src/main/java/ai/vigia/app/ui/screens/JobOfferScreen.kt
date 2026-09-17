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
            InfoChip("Offres d'emploi & formation", VigiaSecondary)
        }

        Column {
            Text("Vérifier une offre d'emploi", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Détecte les faux recruteurs, les frais exigés avant embauche et les promesses de salaire irréalistes.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        GlassCard(
            borderColor = Color(0xFFDBEAFE),
            backgroundColor = Color(0xFFEFF6FF)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = VigiaPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Règle universelle", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaPrimary, fontSize = 13.5.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "On ne paie jamais pour être recruté. Un vrai employeur ne demande ni frais de dossier, ni frais de formation avant l'embauche.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        GlassCard {
            Text("Détails de l'offre", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))

            VigiaField(
                value = companyName,
                onValueChange = { companyName = it },
                label = "Nom de l'entreprise ou du recruteur",
                supporting = "Optionnel, aide à vérifier la légitimité"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = contactEmail,
                onValueChange = { contactEmail = it },
                label = "Email ou contact utilisé (ex: Gmail, WhatsApp...)"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = salaryPromised,
                onValueChange = { salaryPromised = it },
                label = "Salaire ou rémunération promise"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = feeRequested,
                onValueChange = { feeRequested = it },
                label = "Frais demandés avant embauche (le cas échéant)",
                supporting = "Frais de dossier, formation, matériel, uniforme..."
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = content,
                onValueChange = { content = it },
                label = "Texte complet de l'offre reçue",
                minLines = 4,
                singleLine = false,
                supporting = "Colle l'annonce, le message ou l'email reçu"
            )

            Spacer(Modifier.height(16.dp))

            GradientButton(
                text = "Analyser cette offre",
                onClick = {
                    viewModel.verify(content, companyName, contactEmail, salaryPromised, feeRequested)
                },
                loading = state.loading,
                enabled = content.isNotBlank(),
                icon = Icons.Rounded.VerifiedUser,
                modifier = Modifier.fillMaxWidth()
            )
        }

        state.error?.let {
            ErrorBanner(it, onRetry = {
                viewModel.verify(content, companyName, contactEmail, salaryPromised, feeRequested)
            })
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
                    Text("Checklist avant d'accepter :", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaPrimary, fontSize = 14.sp)
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
