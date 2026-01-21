package dk.dtu.padelbattle.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.dtu.padelbattle.ui.theme.*
import dk.dtu.padelbattle.viewmodel.PlayerStanding
import kotlin.random.Random

/**
 * Data class til konfetti partikel
 */
private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val color: Color,
    val size: Float,
    val shape: ConfettiShape
)

private enum class ConfettiShape {
    RECTANGLE, CIRCLE, TRIANGLE
}

/**
 * Konfetti animation komponent
 */
@Composable
private fun ConfettiEffect(
    modifier: Modifier = Modifier,
    isActive: Boolean
) {
    val confettiColors = listOf(
        PadelOrange,
        WarmGold,
        GoldPodium,
        Color(0xFF4CAF50),
        Color(0xFF2196F3),
        Color(0xFFE91E63),
        Color(0xFF9C27B0),
        Color(0xFFFF5722)
    )

    var particles by remember { mutableStateOf<List<ConfettiParticle>>(emptyList()) }

    // Initialiser konfetti partikler når aktiv
    LaunchedEffect(isActive) {
        if (isActive) {
            particles = (0..80).map {
                ConfettiParticle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat() * -0.5f - 0.1f, // Start over skærmen
                    velocityX = (Random.nextFloat() - 0.5f) * 0.02f,
                    velocityY = Random.nextFloat() * 0.008f + 0.004f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = (Random.nextFloat() - 0.5f) * 10f,
                    color = confettiColors.random(),
                    size = Random.nextFloat() * 12f + 6f,
                    shape = ConfettiShape.entries.random()
                )
            }
        }
    }

    // Animation loop
    val infiniteTransition = rememberInfiniteTransition()
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Opdater partikler
    LaunchedEffect(animationProgress) {
        if (isActive && particles.isNotEmpty()) {
            particles = particles.map { particle ->
                particle.copy(
                    x = particle.x + particle.velocityX,
                    y = particle.y + particle.velocityY,
                    rotation = particle.rotation + particle.rotationSpeed,
                    velocityY = particle.velocityY + 0.0002f // Gravitation
                )
            }.filter { it.y < 1.2f } // Fjern partikler der er faldet ud af skærmen
        }
    }

    if (isActive) {
        Canvas(modifier = modifier.fillMaxSize()) {
            particles.forEach { particle ->
                val x = particle.x * size.width
                val y = particle.y * size.height

                rotate(particle.rotation, pivot = Offset(x, y)) {
                    when (particle.shape) {
                        ConfettiShape.RECTANGLE -> {
                            drawRect(
                                color = particle.color,
                                topLeft = Offset(x - particle.size / 2, y - particle.size / 4),
                                size = androidx.compose.ui.geometry.Size(particle.size, particle.size / 2)
                            )
                        }
                        ConfettiShape.CIRCLE -> {
                            drawCircle(
                                color = particle.color,
                                radius = particle.size / 2,
                                center = Offset(x, y)
                            )
                        }
                        ConfettiShape.TRIANGLE -> {
                            val path = Path().apply {
                                moveTo(x, y - particle.size / 2)
                                lineTo(x + particle.size / 2, y + particle.size / 2)
                                lineTo(x - particle.size / 2, y + particle.size / 2)
                                close()
                            }
                            drawPath(path, particle.color)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Podium komponent der viser top 3
 */
@Composable
private fun Podium(
    winner: PlayerStanding,
    second: PlayerStanding?,
    third: PlayerStanding?,
    modifier: Modifier = Modifier
) {
    val podiumHeight1 = 100.dp
    val podiumHeight2 = 75.dp
    val podiumHeight3 = 55.dp

    // Animation for podium
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2. plads (venstre)
        if (second != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "🥈",
                    fontSize = 32.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = second.player.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = "${second.displayTotal} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(podiumHeight2)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(SilverPodium, SilverPodium.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "2",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // 1. plads (center - højest)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1.2f)
        ) {
            Text(
                text = "👑",
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "🥇",
                fontSize = 36.sp
            )
            Text(
                text = winner.player.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = "${winner.displayTotal} pts",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(podiumHeight1)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(GoldPodium, WarmGold)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "1",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // 3. plads (højre)
        if (third != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "🥉",
                    fontSize = 28.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = third.player.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = "${third.displayTotal} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(podiumHeight3)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(BronzePodium, BronzePodium.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "3",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Hoved vinder celebration popup
 */
@Composable
fun WinnerCelebrationPopup(
    isVisible: Boolean,
    winner: PlayerStanding?,
    second: PlayerStanding?,
    third: PlayerStanding?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && winner != null,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Konfetti effekt
            ConfettiEffect(
                modifier = Modifier.fillMaxSize(),
                isActive = isVisible
            )

            // Popup indhold
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .shadow(24.dp, RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Forhindre at klik på popup lukker den */ },
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    PadelOrange,
                                    PadelOrangeDark,
                                    Color(0xFF1A1512)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Titel
                        Text(
                            text = "🏆 VINDER! 🏆",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = WarmGold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Turneringen er afsluttet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Podium med top 3
                        if (winner != null) {
                            Podium(
                                winner = winner,
                                second = second,
                                third = third
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Instruktion om at lukke
                        Text(
                            text = "Tryk hvor som helst for at lukke",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

