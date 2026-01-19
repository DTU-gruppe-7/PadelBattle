package dk.dtu.padelbattle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// FARVEPALET - Orange/Varm tema inspireret af PadelBattle logo
// ============================================================================

// Primære orange farver
val PadelOrange = Color(0xFFFF6B35)           // Vibrant orange - hovedfarve
val PadelOrangeDark = Color(0xFFE55A2B)       // Mørkere orange til kontrast
val PadelOrangeLight = Color(0xFFFF8A5C)      // Lysere orange til highlights

// Sekundære farver 
val WarmGold = Color(0xFFFFB74D)              // Varm guld til vindere/accents
val DeepAmber = Color(0xFFFF8F00)             // Dyb amber

// Baggrunde (Light mode)
val LightBackground = Color(0xFFFFFBF8)       // Varm off-white
val LightSurface = Color(0xFFFFFFFF)          // Ren hvid til kort
val LightSurfaceVariant = Color(0xFFFFF3E0)   // Let orange tint

// Baggrunde (Dark mode)
val DarkBackground = Color(0xFF1A1512)        // Varm mørk baggrund
val DarkSurface = Color(0xFF2D2520)           // Varm mørk surface
val DarkSurfaceVariant = Color(0xFF3D3530)    // Lidt lysere

// Tekst farver
val OnLightPrimary = Color(0xFFFFFFFF)        // Hvid på orange
val OnDarkPrimary = Color(0xFFFFFFFF)         // Hvid på orange
val OnLightBackground = Color(0xFF1C1B1F)     // Næsten sort
val OnDarkBackground = Color(0xFFF5F5F5)      // Off-white

// Statusfarver
val SuccessGreen = Color(0xFF4CAF50)          // Grøn til succes
val ErrorRed = Color(0xFFE53935)              // Rød til fejl
val WarningAmber = Color(0xFFFFB300)          // Amber til advarsler

// Podie-farver til StandingsScreen
val GoldPodium = Color(0xFFFFD700)            // Guld - 1. plads
val SilverPodium = Color(0xFFC0C0C0)          // Sølv - 2. plads
val BronzePodium = Color(0xFFCD7F32)          // Bronze - 3. plads

// ============================================================================
// COLOR SCHEMES
// ============================================================================

private val LightColorScheme = lightColorScheme(
    primary = PadelOrange,
    onPrimary = OnLightPrimary,
    primaryContainer = Color(0xFFFFE0D0),
    onPrimaryContainer = Color(0xFF3D1800),
    
    secondary = DeepAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF3D2800),
    
    tertiary = WarmGold,
    onTertiary = Color(0xFF3D2800),
    tertiaryContainer = Color(0xFFFFECCC),
    onTertiaryContainer = Color(0xFF3D2800),
    
    background = LightBackground,
    onBackground = OnLightBackground,
    
    surface = LightSurface,
    onSurface = OnLightBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF5D5D5D),
    
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    outline = Color(0xFFE0D5CC),
    outlineVariant = Color(0xFFF0E8E0)
)

private val DarkColorScheme = darkColorScheme(
    primary = PadelOrangeLight,
    onPrimary = Color(0xFF3D1800),
    primaryContainer = PadelOrangeDark,
    onPrimaryContainer = Color(0xFFFFE0D0),
    
    secondary = WarmGold,
    onSecondary = Color(0xFF3D2800),
    secondaryContainer = Color(0xFF5D4200),
    onSecondaryContainer = Color(0xFFFFE0B2),
    
    tertiary = Color(0xFFFFCC80),
    onTertiary = Color(0xFF3D2800),
    tertiaryContainer = Color(0xFF5D4200),
    onTertiaryContainer = Color(0xFFFFECCC),
    
    background = DarkBackground,
    onBackground = OnDarkBackground,
    
    surface = DarkSurface,
    onSurface = OnDarkBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB0A8A0),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    outline = Color(0xFF5D5550),
    outlineVariant = Color(0xFF4D4540)
)

// ============================================================================
// TYPOGRAPHY
// ============================================================================

val PadelBattleTypography = Typography(
    // Display styles - store overskrifter
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    
    // Headline styles - sektionsoverskrifter
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    
    // Title styles - kortere overskrifter
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // Body styles - brødtekst
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    
    // Label styles - små tekster
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// ============================================================================
// SHAPES
// ============================================================================

val PadelBattleShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// ============================================================================
// THEME COMPOSABLE
// ============================================================================

@Composable
fun PadelBattleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PadelBattleTypography,
        shapes = PadelBattleShapes,
        content = content
    )
}
