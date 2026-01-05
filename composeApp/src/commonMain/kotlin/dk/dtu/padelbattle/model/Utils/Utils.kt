package dk.dtu.padelbattle.model.Utils

fun generateId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..16)
        .map { chars.random() }
        .joinToString("")
}