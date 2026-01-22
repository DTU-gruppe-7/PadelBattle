package dk.dtu.padelbattle.domain.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.dateWithTimeIntervalSince1970

actual fun formatDate(timestamp: Long): String {
    // Konverter millisekunder (Kotlin) til sekunder (iOS)
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)

    val formatter = NSDateFormatter()
    // ShortStyle giver typisk "dd/MM/yyyy" afhængigt af brugerens region
    formatter.dateStyle = NSDateFormatterShortStyle

    return formatter.stringFromDate(date)
}
