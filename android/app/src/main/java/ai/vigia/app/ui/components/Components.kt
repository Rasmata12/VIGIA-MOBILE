package ai.vigia.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.theme.*

/** Carte moderne blanche épurée avec ombre douce et bordure fine, avec une légère
 * entrée en fondu + montée pour que l'app se sente vivante plutôt que statique. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = VigiaBorder,
    borderBrush: Brush? = null,
    backgroundColor: Color? = null,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 3.dp,
    animateEntrance: Boolean = true,
    entranceDelayMillis: Long = 0,
    content: @Composable ColumnScope.() -> Unit
) {
    val effectiveBorder = borderBrush?.let { BorderStroke(1.2.dp, it) } ?: BorderStroke(1.dp, borderColor)
    val backgroundBrush = if (backgroundColor != null) {
        Brush.verticalGradient(listOf(backgroundColor, backgroundColor))
    } else {
        CardGlassGradient
    }

    var appeared by remember { mutableStateOf(!animateEntrance) }
    LaunchedEffect(Unit) {
        if (entranceDelayMillis > 0) kotlinx.coroutines.delay(entranceDelayMillis)
        appeared = true
    }
    val entranceProgress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "cardEntrance"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                alpha = entranceProgress
                translationY = (1f - entranceProgress) * 18f
            }
            .shadow(elevation, RoundedCornerShape(cornerRadius), ambientColor = Color(0x0A0F172A), spotColor = Color(0x0F0F172A))
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundBrush)
            .border(effectiveBorder, RoundedCornerShape(cornerRadius))
            .padding(18.dp),
        content = content
    )
}

/** Bouton d'action avec dégradé bleu royal / indigo et typographie Poppins */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    gradient: Brush = BrandGradient
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "buttonPressScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .height(52.dp)
            .shadow(if (enabled && !loading) 6.dp else 0.dp, RoundedCornerShape(16.dp), ambientColor = VigiaPrimary.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled && !loading) gradient
                else Brush.horizontalGradient(listOf(VigiaBorder, VigiaBorder))
            )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContentColor = VigiaTextMuted
            ),
            elevation = null
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(Modifier.width(12.dp))
                Text("Vérification en cours…", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.3.sp)
                }
            }
        }
    }
}

/** Jauge de risque haute précision lumineuse avec animation fluide */
@Composable
fun RiskGauge(
    score: Int,
    level: String,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val progress by animateFloatAsState(
        targetValue = (score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "gaugeProgress"
    )
    val color = riskColor(level)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)

            // Arc d'arrière-plan clair
            drawArc(
                color = Color(0xFFE2E8F0),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Arc principal de valeur
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        color.copy(alpha = 0.6f),
                        color,
                        color
                    )
                ),
                startAngle = 135f,
                sweepAngle = (270f * progress).coerceAtLeast(2f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$score",
                    fontSize = 44.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = VigiaTextPrimary,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "/100",
                    fontSize = 13.5.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = VigiaTextMuted,
                    modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "INDICE DE RISQUE",
                fontSize = 9.5.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                color = VigiaTextSecondary,
                letterSpacing = 1.1.sp
            )
            Spacer(Modifier.height(8.dp))
            LevelBadge(level)
        }
    }
}

/** Badge tactique de niveau de risque (Sûr, Suspect, Dangereux) */
@Composable
fun LevelBadge(level: String, modifier: Modifier = Modifier) {
    val color = riskColor(level)
    val bg = riskBackground(level)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = riskLabel(level).uppercase(),
                color = color,
                fontSize = 10.5.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

/** Champ de saisie moderne avec fond blanc épuré et bordure soignée */
@Composable
fun VigiaField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    minLines: Int = 1,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    supporting: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontFamily = PoppinsFontFamily) },
        leadingIcon = leadingIcon,
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Afficher mot de passe",
                        tint = VigiaTextSecondary
                    )
                }
            } else if (value.isNotEmpty() && singleLine) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Effacer",
                        tint = VigiaTextMuted
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(16.dp),
        supportingText = supporting?.let { { Text(it, fontSize = 12.sp, fontFamily = PoppinsFontFamily, color = VigiaTextMuted) } },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation()
        else VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VigiaPrimary,
            unfocusedBorderColor = VigiaBorder,
            focusedContainerColor = VigiaWhite,
            unfocusedContainerColor = VigiaWhite,
            focusedLabelColor = VigiaPrimary,
            unfocusedLabelColor = VigiaTextSecondary,
            cursorColor = VigiaPrimary
        )
    )
}

/** État vide stylisé avec icône vectorielle moderne et message clair */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFF6FF))
                .border(1.dp, Color(0xFFDBEAFE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = VigiaPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = PoppinsFontFamily,
            color = VigiaTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = PoppinsFontFamily,
            color = VigiaTextSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel, color = VigiaPrimary, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Bannière d'erreur avec icône vectorielle Warning et action Retry */
@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = RiskDangerBorder,
        backgroundColor = RiskDangerBg
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = "Erreur",
                tint = RiskDanger,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(message, color = VigiaTextPrimary, style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily)
                if (onRetry != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Toucher pour réessayer",
                        color = VigiaPrimary,
                        fontSize = 12.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onRetry() }
                    )
                }
            }
        }
    }
}

/** Pastille d'information avec accent moderne */
@Composable
fun InfoChip(text: String, color: Color = VigiaPrimary) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
    }
}

/** Carte métrique stylisée avec valeur proéminente */
@Composable
fun StatTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    GlassCard(
        modifier = modifier,
        borderColor = VigiaBorder
    ) {
        Text(
            text = value,
            fontSize = 26.sp,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            letterSpacing = (-0.5).sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            color = VigiaTextPrimary
        )
        if (subtitle != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                fontFamily = PoppinsFontFamily,
                color = VigiaTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** En-tête de section avec barre d'accent */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(VigiaPrimary)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = PoppinsFontFamily,
                color = VigiaTextPrimary,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = VigiaPrimary, fontFamily = PoppinsFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Bannière de décision haute visibilité pour Before Pay avec icônes vectorielles */
@Composable
fun DecisionBanner(
    decision: String,
    headline: String,
    modifier: Modifier = Modifier
) {
    val (color, title, iconVector) = when (decision.lowercase()) {
        "stop" -> Triple(RiskDanger, "NE TRANSFÈRE PAS D'ARGENT", Icons.Rounded.Cancel)
        "verifier" -> Triple(RiskSuspicious, "VÉRIFICATION INDISPENSABLE", Icons.Rounded.Warning)
        else -> Triple(RiskSafe, "PRUDENCE RECOMMANDÉE", Icons.Rounded.Shield)
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = color.copy(alpha = 0.4f),
        backgroundColor = riskBackground(decision)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = color,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = headline,
                    color = VigiaTextPrimary,
                    fontFamily = PoppinsFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** Carte d'un trait Scam DNA avec force et preuves */
@Composable
fun ScamDnaCard(
    category: String,
    label: String,
    strength: Int,
    evidence: List<String>,
    modifier: Modifier = Modifier
) {
    val traitColor = if (strength >= 70) RiskDanger else if (strength >= 40) RiskSuspicious else VigiaPrimary

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = VigiaBorder
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = VigiaTextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "CATÉGORIE: $category".uppercase(),
                    fontSize = 10.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = VigiaTextMuted,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            InfoChip(
                text = "Force $strength%",
                color = traitColor
            )
        }

        Spacer(Modifier.height(10.dp))

        // Barre d'intensité du trait
        LinearProgressIndicator(
            progress = { strength / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = traitColor,
            trackColor = Color(0xFFE2E8F0)
        )

        if (evidence.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            evidence.forEach { ev ->
                Text(
                    text = "• $ev",
                    fontSize = 12.sp,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/** Radar animé simulant un scan cybernétique continu (pour Guard) */
@Composable
fun CyberRadarView(
    active: Boolean,
    modifier: Modifier = Modifier,
    radarSize: Dp = 130.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarSweep"
    )

    Box(modifier = modifier.size(radarSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.width / 2

            // Cercles concentriques clairs
            drawCircle(color = Color(0xFFDBEAFE), radius = radius * 0.35f, style = Stroke(1.dp.toPx()))
            drawCircle(color = Color(0xFFDBEAFE), radius = radius * 0.70f, style = Stroke(1.dp.toPx()))
            drawCircle(color = Color(0xFFBFDBFE), radius = radius, style = Stroke(1.2.dp.toPx()))

            if (active) {
                // Balayage lumineux bleu royal
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            VigiaPrimary.copy(alpha = 0.05f),
                            VigiaPrimary.copy(alpha = 0.35f)
                        )
                    ),
                    startAngle = sweepAngle - 45f,
                    sweepAngle = 45f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = this.size
                )
            }
        }

        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (active) Color(0xFFEFF6FF) else Color(0xFFF1F5F9))
                .border(
                    1.dp,
                    if (active) VigiaPrimary else VigiaBorder,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (active) Icons.Rounded.Security else Icons.Rounded.Pause,
                contentDescription = null,
                tint = if (active) VigiaPrimary else VigiaTextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
