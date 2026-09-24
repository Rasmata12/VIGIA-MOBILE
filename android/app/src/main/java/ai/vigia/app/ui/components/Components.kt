package ai.vigia.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.theme.*

/** Carte premium avec ombre douce TEINTÉE selon son contexte (une carte au bord
 * rouge projette une ombre rouge très légère, une carte violette une ombre violette...),
 * un fin liseré d'accent en haut, et un dégradé de fond perceptible — pour que chaque
 * carte se sente distincte plutôt que d'être un bloc blanc générique répété partout.
 * Entrée en fondu + montée pour que l'app se sente vivante plutôt que statique. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = VigiaBorder,
    borderBrush: Brush? = null,
    backgroundColor: Color? = null,
    backgroundBrush: Brush? = null,
    cornerRadius: Dp = 22.dp,
    elevation: Dp = 4.dp,
    accentTop: Boolean = true,
    animateEntrance: Boolean = true,
    entranceDelayMillis: Long = 0,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val effectiveBorder = borderBrush?.let { BorderStroke(1.2.dp, it) } ?: BorderStroke(1.dp, borderColor)
    val effectiveBackground = backgroundBrush ?: if (backgroundColor != null) {
        SolidColor(backgroundColor)
    } else {
        CardGlassGradient
    }
    // L'ombre porte une teinte de la couleur de bordure de la carte (si elle en a une
    // qui n'est pas le gris neutre par defaut), pour un effet "halo" propre a chaque carte.
    val shadowTint = Color(0xFF0F172A)

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
            .shadow(elevation, RoundedCornerShape(cornerRadius), ambientColor = Color(0x060F172A), spotColor = Color(0x0A0F172A))
            .clip(RoundedCornerShape(cornerRadius))
            .background(effectiveBackground)
            .border(effectiveBorder, RoundedCornerShape(cornerRadius))
    ) {
        if (accentTop && borderColor != VigiaBorder) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(SolidColor(borderColor))
            )
        }
        Column(Modifier.padding(contentPadding), content = content)
    }
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
                else SolidColor(VigiaBorder)
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
                color = color,
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

/** Carte métrique de luxe avec valeur proéminente, micro-lueur et indicateur de statut */
@Composable
fun StatTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    badgeText: String? = null
) {
    GlassCard(
        modifier = modifier,
        borderBrush = luxuryBorderGradient(color),
        backgroundBrush = luxuryCardGradient(color),
        cornerRadius = 20.dp,
        elevation = 3.dp,
        accentTop = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        badgeText,
                        fontSize = 9.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = value,
            fontSize = 26.sp,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            letterSpacing = (-0.8).sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            color = VigiaTextPrimary,
            lineHeight = 16.sp
        )
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                lineHeight = 14.sp
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
                    color = VigiaPrimary.copy(alpha = 0.18f),
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

/** Bouton retour discret, en pill ou chip circulaire semi-transparente avec un chevron fin */
@Composable
fun SubtleBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Box(
        modifier = modifier
            .then(
                if (label != null) Modifier.height(38.dp)
                else Modifier.size(40.dp)
            )
            .shadow(4.dp, if (label != null) RoundedCornerShape(19.dp) else CircleShape, ambientColor = Color(0x18000000))
            .clip(if (label != null) RoundedCornerShape(19.dp) else CircleShape)
            .background(Color.White)
            .border(1.dp, VigiaBorder, if (label != null) RoundedCornerShape(19.dp) else CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onBack() }
            .then(if (label != null) Modifier.padding(horizontal = 12.dp) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.ArrowBackIosNew,
                contentDescription = label ?: "Retour",
                tint = VigiaTextPrimary,
                modifier = Modifier.size(14.dp)
            )
            if (label != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = VigiaTextPrimary
                )
            }
        }
    }
}


// =========================================================================
// SYSTÈME DE DIRECTION ARTISTIQUE — primitives premium réutilisables
// Pour donner une vraie identité visuelle aux écrans sans photographie :
// halos ambiants, badges en dégradé, cartes héros, grilles bento.
// =========================================================================

/** Halo de lumière douce en dégradé radial — décor d'ambiance, pas de blur réel
 * (compatibilité minSdk 26), juste une tache de couleur qui s'estompe en douceur. */
@Composable
fun GlowOrb(color: Color, modifier: Modifier = Modifier, intensity: Float = 0.28f) {
    Box(
        modifier = modifier.background(
            color = color.copy(alpha = intensity),
            shape = CircleShape
        )
    )
}

/** Badge d'icône raffiné : forme "squircle", fond en léger dégradé, fin liseré —
 * remplace les cercles plats répétés partout dans l'app. */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    cornerRadius: Dp = 14.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(cornerRadius)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.20f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Panneau héros premium : surface claire en dégradé subtil, avec 1 à 2 halos de
 * couleur en fond pour donner de la profondeur — le "moment fort" visuel d'un
 * écran qui n'a pas de photographie (Dashboard, Guard, Before Pay, Moment...). */
@Composable
fun HeroSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    orbColors: List<Color> = listOf(VigiaPrimary, VigiaViolet),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(cornerRadius), ambientColor = VigiaPrimary.copy(alpha = 0.16f), spotColor = VigiaPrimary.copy(alpha = 0.10f))
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White)
    ) {
        content()
    }
}

/** Tuile "bento" de luxe pour grilles d'actions asymétriques :
 * avec fond coloré subtil, bordure néo-prismatique, icône en filigrane géante,
 * micro-badge de catégorie et typographie Poppins nette. */
@Composable
fun BentoActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    drawableRes: Int? = null,
    color: Color,
    modifier: Modifier = Modifier,
    tag: String? = null,
    featured: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bentoCardPress"
    )

    GlassCard(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        borderBrush = luxuryBorderGradient(color),
        backgroundBrush = luxuryCardGradient(color),
        cornerRadius = 24.dp,
        elevation = if (featured) 5.dp else 3.dp,
        accentTop = true
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.08f),
                    modifier = Modifier
                        .size(if (featured) 110.dp else 70.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = if (featured) 18.dp else 12.dp, y = (-12).dp)
                )
            } else if (drawableRes != null) {
                Icon(
                    painter = painterResource(drawableRes),
                    contentDescription = null,
                    tint = color.copy(alpha = 0.08f),
                    modifier = Modifier
                        .size(if (featured) 110.dp else 70.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = if (featured) 18.dp else 12.dp, y = (-12).dp)
                )
            }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (drawableRes != null) {
                        Box(
                            modifier = Modifier
                                .size(if (featured) 50.dp else 40.dp)
                                .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = Color(0x0E000000))
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, color.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(drawableRes),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(if (featured) 28.dp else 22.dp)
                            )
                        }
                    } else if (icon != null) {
                        IconBadge(
                            icon = icon,
                            tint = color,
                            size = if (featured) 50.dp else 40.dp,
                            iconSize = if (featured) 24.dp else 18.dp
                        )
                    }
                    if (tag != null || featured) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(color.copy(alpha = 0.12f))
                                .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = (tag ?: if (featured) "RECOMMANDÉ" else "ACTIF").uppercase(),
                                fontSize = 9.sp,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                color = color,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(if (featured) 14.dp else 10.dp))
                Text(
                    text = title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = VigiaTextPrimary,
                    fontSize = if (featured) 17.5.sp else 14.sp,
                    lineHeight = if (featured) 22.sp else 19.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontFamily = PoppinsFontFamily,
                    fontSize = if (featured) 12.5.sp else 11.5.sp,
                    color = VigiaTextSecondary,
                    lineHeight = if (featured) 18.sp else 16.sp
                )
                if (featured) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Analyser maintenant",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = color
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

/** Pastille tactile de sélection d'opérateur Mobile Money & Carte avec icône réelle ou badge */
@Composable
fun OperatorChip(
    name: String,
    drawableRes: Int? = null,
    badgeText: String? = null,
    icon: ImageVector? = null,
    selected: Boolean,
    color: Color = VigiaPrimary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color.copy(alpha = 0.08f) else Color.White)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) color else VigiaBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (drawableRes != null) {
                Icon(
                    painter = painterResource(drawableRes),
                    contentDescription = name,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(7.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) color else VigiaTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(7.dp))
            } else if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (selected) color else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = if (selected) Color.White else VigiaTextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PoppinsFontFamily
                    )
                }
                Spacer(Modifier.width(7.dp))
            }
            Text(
                text = name,
                fontFamily = PoppinsFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                color = if (selected) color else VigiaTextPrimary
            )
        }
    }
}

/** Viseur de scanner holographique avec balayage laser animé et coins de cadrage néon */
@Composable
fun CameraScannerOverlay(
    modifier: Modifier = Modifier,
    isScanning: Boolean = true,
    errorDetected: Boolean = false,
    statusText: String = "Viseur prêt • Pointez vers un QR Code ou un code-barres",
    onScanClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laserScan")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserPos"
    )

    val laserColor = if (errorDetected) RiskDanger else VigiaPrimaryBright

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .shadow(12.dp, RoundedCornerShape(26.dp), ambientColor = laserColor.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF0F172A))
            .border(1.5.dp, laserColor.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
            .clickable { onScanClick() },
        contentAlignment = Alignment.Center
    ) {
        // Grille de fond subtile
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 32.dp.toPx()
            for (x in 0..(size.width / step).toInt()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.04f),
                    start = Offset(x * step, 0f),
                    end = Offset(x * step, size.height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(size.height / step).toInt()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.04f),
                    start = Offset(0f, y * step),
                    end = Offset(size.width, y * step),
                    strokeWidth = 1f
                )
            }

            // Coins de visée
            val cornerLength = 28.dp.toPx()
            val strokeWidth = 3.5.dp.toPx()
            val pad = 42.dp.toPx()
            val w = size.width
            val h = size.height

            // Haut Gauche
            drawLine(laserColor, Offset(pad, pad), Offset(pad + cornerLength, pad), strokeWidth, StrokeCap.Round)
            drawLine(laserColor, Offset(pad, pad), Offset(pad, pad + cornerLength), strokeWidth, StrokeCap.Round)

            // Haut Droite
            drawLine(laserColor, Offset(w - pad, pad), Offset(w - pad - cornerLength, pad), strokeWidth, StrokeCap.Round)
            drawLine(laserColor, Offset(w - pad, pad), Offset(w - pad, pad + cornerLength), strokeWidth, StrokeCap.Round)

            // Bas Gauche
            drawLine(laserColor, Offset(pad, h - pad), Offset(pad + cornerLength, h - pad), strokeWidth, StrokeCap.Round)
            drawLine(laserColor, Offset(pad, h - pad), Offset(pad, h - pad - cornerLength), strokeWidth, StrokeCap.Round)

            // Bas Droite
            drawLine(laserColor, Offset(w - pad, h - pad), Offset(w - pad - cornerLength, h - pad), strokeWidth, StrokeCap.Round)
            drawLine(laserColor, Offset(w - pad, h - pad), Offset(w - pad, h - pad - cornerLength), strokeWidth, StrokeCap.Round)

            // Ligne Laser animée
            if (isScanning) {
                val yLaser = pad + (h - 2 * pad) * laserPosition
                drawLine(
                    color = laserColor.copy(alpha = 0.85f),
                    start = Offset(pad, yLaser),
                    end = Offset(w - pad, yLaser),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Cœur de visée
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(laserColor.copy(alpha = 0.18f))
                    .border(1.dp, laserColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (errorDetected) Icons.Rounded.Warning else Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = laserColor,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, laserColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 11.5.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Carte interactive "Leçon & Réflexe Cyber" avec design flashcard ultra-soigné */
@Composable
fun SecurityLessonCard(
    title: String,
    category: String,
    takeaway: String,
    tips: List<String>,
    color: Color = VigiaPrimary,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(240, easing = FastOutSlowInEasing))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { expanded = !expanded },
        borderBrush = luxuryBorderGradient(color),
        backgroundBrush = luxuryCardGradient(color),
        cornerRadius = 22.dp,
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = category.uppercase(),
                    fontSize = 9.5.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    letterSpacing = 0.8.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Réduire le conseil" else "Dérouler le conseil",
                    tint = VigiaTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            color = VigiaTextPrimary,
            fontSize = 15.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = takeaway,
            fontFamily = PoppinsFontFamily,
            fontSize = 12.5.sp,
            color = VigiaTextSecondary,
            lineHeight = 18.sp
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = color.copy(alpha = 0.15f), thickness = 1.dp)
                Spacer(Modifier.height(10.dp))
                tips.forEach { tip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = tip,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 12.sp,
                            color = VigiaTextPrimary,
                            lineHeight = 16.5.sp
                        )
                    }
                }
            }
        }
    }
}

/** Ligne de raccourci "spotlight" avec liseré d'accent et icône filigrane */
@Composable
fun SpotlightRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.clickable { onClick() },
        borderBrush = luxuryBorderGradient(color),
        backgroundBrush = luxuryCardGradient(color),
        cornerRadius = 20.dp
    ) {
        Box(Modifier.fillMaxWidth()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(84.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = icon, tint = color, size = 44.dp, iconSize = 21.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.5.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
    }
}
