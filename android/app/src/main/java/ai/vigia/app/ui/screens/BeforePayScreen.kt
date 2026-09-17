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
import ai.vigia.app.ui.vm.BeforePayViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BeforePayScreen(
    viewModel: BeforePayViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var message by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var beneficiary by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("") }

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
            InfoChip("Anti-Arnaque Transfert", VigiaViolet)
        }

        Column {
            Text("Vérifier avant de payer", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Analyse les risques de fraude sur un transfert Mobile Money, un virement ou un paiement en ligne.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        // Avertissement de sécurité absolu
        GlassCard(
            borderColor = Color(0xFFDBEAFE),
            backgroundColor = Color(0xFFEFF6FF)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = VigiaPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Garantie de Sécurité & Confidentialité", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaPrimary, fontSize = 13.5.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Ne saisis JAMAIS ton code PIN, ton mot de passe ou tes numéros de carte bancaire. VIGIA refuse et rejette toute saisie contenant ces informations.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        GlassCard {
            Text("Informations du transfert", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))

            VigiaField(
                value = beneficiary,
                onValueChange = { beneficiary = it },
                label = "Nom ou numéro du bénéficiaire (ex: +226..., Orange Money)",
                supporting = "Optionnel mais fortement recommandé pour vérifier la cohérence"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = amount,
                onValueChange = { amount = it },
                label = "Montant demandé (ex: 25 000 FCFA, 50 EUR)"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = message,
                onValueChange = { message = it },
                label = "Message ou SMS reçu demandant le paiement",
                minLines = 3,
                singleLine = false,
                supporting = "Colle ici la demande reçue (SMS, WhatsApp, email de relance)"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = url,
                onValueChange = { url = it },
                label = "Lien de paiement éventuel (URL reçue)",
                supporting = "Ex: lien de recharge ou fausse page de validation"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = context,
                onValueChange = { context = it },
                label = "Contexte (ex: achat marketplace, avance frais de livraison, loterie)",
                supporting = "Aide à identifier les scénarios typiques d'escroquerie"
            )

            Spacer(Modifier.height(16.dp))

            GradientButton(
                text = "Vérifier le paiement maintenant",
                onClick = {
                    viewModel.verify(
                        message = message,
                        url = url,
                        beneficiary = beneficiary,
                        amount = amount,
                        context = context
                    )
                },
                loading = state.loading,
                enabled = message.isNotBlank() || url.isNotBlank(),
                icon = Icons.Rounded.VerifiedUser,
                modifier = Modifier.fillMaxWidth()
            )
        }

        state.error?.let {
            ErrorBanner(it, onRetry = {
                viewModel.verify(message, url, beneficiary, amount, context)
            })
        }

        state.result?.let { res ->
            SectionHeader("Verdict de Sécurité")

            DecisionBanner(
                decision = res.decision,
                headline = res.headline
            )

            if (res.beneficiaryNotes.isNotEmpty()) {
                GlassCard(borderColor = RiskSuspiciousBorder, backgroundColor = RiskSuspiciousBg) {
                    Text("Remarques sur le destinataire", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskSuspicious, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    res.beneficiaryNotes.forEach { note ->
                        Text("• $note", style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
                    }
                }
            }

            res.assessment?.let { assessment ->
                GlassCard {
                    Text("Analyse du risque transactionnel", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        RiskGauge(score = assessment.score, level = assessment.level, size = 160.dp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(assessment.summary, style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary)
                }

                if (assessment.scamDna.isNotEmpty()) {
                    SectionHeader("Marqueurs de manipulation détectés (Scam DNA)")
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
                    borderColor = Color(0xFFDBEAFE),
                    backgroundColor = Color(0xFFEFF6FF)
                ) {
                    Text("Checklist obligatoire avant de transférer :", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaPrimary, fontSize = 14.sp)
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
