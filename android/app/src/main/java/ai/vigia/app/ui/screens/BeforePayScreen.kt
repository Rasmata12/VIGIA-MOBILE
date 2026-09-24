package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.R
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.BeforePayViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private data class OperatorItem(
    val name: String,
    val drawableRes: Int? = null,
    val badge: String? = null,
    val icon: ImageVector? = null,
    val color: Color
)

@Composable
fun BeforePayScreen(
    viewModel: BeforePayViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var selectedOperator by remember { mutableStateOf("Wave") }
    var message by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var beneficiary by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("") }

    val operators = listOf(
        OperatorItem("Wave", drawableRes = R.drawable.ic_wave, color = Color(0xFF00B2FE)),
        OperatorItem("Orange Money", drawableRes = R.drawable.ic_orange_money, color = Color(0xFFFF6600)),
        OperatorItem("MTN MoMo", drawableRes = R.drawable.ic_mtn_momo, color = Color(0xFFCA8A04)),
        OperatorItem("Moov Money", drawableRes = R.drawable.ic_moov_money, color = Color(0xFF0284C7)),
        OperatorItem("Carte Bancaire", icon = Icons.Rounded.CreditCard, color = VigiaNavy)
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // En-tête sobre et clair
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubtleBackButton(onBack = onBack, label = "Retour")
            Spacer(Modifier.weight(1f))
            InfoChip("Audit Financier", VigiaNavy)
        }

        // Titre concis
        Column {
            Text(
                "Before Pay™",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = VigiaTextPrimary,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Vérifiez un bénéficiaire et un motif avant tout transfert d'argent.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        // Sélecteur d'opérateur Mobile Money (Zero emoji, monogrammes pro)
        Column {
            Text(
                "Canal de paiement",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = VigiaTextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                operators.forEach { op ->
                    OperatorChip(
                        name = op.name,
                        drawableRes = op.drawableRes,
                        badgeText = op.badge,
                        icon = op.icon,
                        selected = selectedOperator == op.name,
                        color = op.color,
                        onClick = {
                            selectedOperator = op.name
                            if (beneficiary.isBlank()) beneficiary = "${op.name} : "
                        }
                    )
                }
            }
        }

        // Règle de sécurité courte
        HeroSurface(orbColors = listOf(VigiaNavy, VigiaPrimary), cornerRadius = 20.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(icon = Icons.Rounded.Shield, tint = VigiaPrimary, size = 40.dp, iconSize = 20.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Règle de sécurité",
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaNavy,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Ne partagez jamais votre code secret. Aucun opérateur ne vous le demandera.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Formulaire d'audit
        GlassCard(
            backgroundBrush = luxuryCardGradient(VigiaNavy),
            borderBrush = luxuryBorderGradient(VigiaNavy),
            cornerRadius = 22.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Rounded.Payment, tint = VigiaPrimary, size = 36.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Détails de l'envoi",
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextPrimary,
                        fontSize = 15.sp
                    )
                }
                TextButton(onClick = {
                    selectedOperator = "Orange Money"
                    beneficiary = "Orange Money (+225 07 00 11 22)"
                    amount = "45 000 FCFA"
                    message = "Félicitations ! Vous avez gagné 500 000 FCFA. Envoyez 45 000 FCFA de frais de dossier pour débloquer votre gain."
                    context = "Faux gain de concours par SMS"
                }) {
                    Text(
                        "Exemple arnaque",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = VigiaPrimary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            VigiaField(
                value = beneficiary,
                onValueChange = { beneficiary = it },
                label = "Destinataire (numéro ou nom)",
                supporting = "Ex: +225 07..., +226 70..., $selectedOperator"
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
                label = "Message ou instruction reçue",
                minLines = 3,
                singleLine = false,
                supporting = "Collez le texte reçu par SMS, WhatsApp ou mail"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = url,
                onValueChange = { url = it },
                label = "Lien reçu (optionnel)",
                supporting = "Ex: lien de paiement ou page web"
            )

            Spacer(Modifier.height(10.dp))

            VigiaField(
                value = context,
                onValueChange = { context = it },
                label = "Contexte (achat, acompte, recrutement, aide...)",
                supporting = "Permet d'évaluer le scénario psychologique"
            )

            Spacer(Modifier.height(16.dp))

            GradientButton(
                text = "Auditer ce paiement",
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
                enabled = message.isNotBlank() || url.isNotBlank() || amount.isNotBlank(),
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
            SectionHeader("Verdict de l'Audit")

            DecisionBanner(
                decision = res.decision,
                headline = res.headline
            )

            if (res.beneficiaryNotes.isNotEmpty()) {
                GlassCard(
                    backgroundBrush = luxuryCardGradient(RiskSuspicious),
                    borderBrush = luxuryBorderGradient(RiskSuspicious),
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Rounded.PersonSearch, tint = RiskSuspicious, size = 34.dp, iconSize = 16.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Analyse du destinataire",
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily,
                            color = RiskSuspicious,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    res.beneficiaryNotes.forEach { note ->
                        Text(
                            text = "• $note",
                            fontFamily = PoppinsFontFamily,
                            color = VigiaTextPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            res.assessment?.let { assessment ->
                GlassCard(
                    backgroundBrush = luxuryCardGradient(VigiaPrimary),
                    borderBrush = luxuryBorderGradient(VigiaPrimary),
                    cornerRadius = 20.dp
                ) {
                    Text(
                        "Niveau de Risque",
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextPrimary,
                        fontSize = 14.5.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        RiskGauge(score = assessment.score, level = assessment.level, size = 150.dp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        assessment.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                }

                if (assessment.scamDna.isNotEmpty()) {
                    SectionHeader("Marqueurs Identifiés (Scam DNA)")
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
                    backgroundBrush = luxuryCardGradient(VigiaNavy),
                    borderBrush = luxuryBorderGradient(VigiaNavy),
                    cornerRadius = 20.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Rounded.FactCheck, tint = VigiaPrimary, size = 32.dp, iconSize = 16.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Conseils de sécurité",
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily,
                            color = VigiaNavy,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    res.checklist.forEachIndexed { idx, point ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Text(
                                "${idx + 1}.",
                                color = VigiaPrimary,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                point,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
