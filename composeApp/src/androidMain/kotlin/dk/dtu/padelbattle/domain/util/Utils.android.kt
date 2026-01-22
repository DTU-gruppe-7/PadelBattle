package dk.dtu.padelbattle.domain.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatDate(timestamp: Long): String {
    // Vælg dit ønskede format, f.eks. "dd/MM/yyyy" eller "dd. MMM yyyy"
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
