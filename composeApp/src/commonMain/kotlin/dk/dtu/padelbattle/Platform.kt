package dk.dtu.padelbattle

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform