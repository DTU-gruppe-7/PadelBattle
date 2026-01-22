package dk.dtu.padelbattle.domain.util

fun generateId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..16)
        .map { chars.random() }
        .joinToString("")
}

expect fun formatDate(timestamp: Long): String
