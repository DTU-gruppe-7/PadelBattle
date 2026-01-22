package dk.dtu.padelbattle.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    val title: String
}

@Serializable
object Home: Screen {
    override val title = "Padel Battle"
}

@Serializable
object ChooseTournament: Screen {
    override val title = "Vælg Turnering"
}

@Serializable
object SearchTournament: Screen {
    override val title = "Søg turneringer"
}

@Serializable
data class TournamentConfig(
    val tournamentType: String,
    val duplicateFromId: String? = null
): Screen {
    override val title = "Opsæt Turnering"
}

@Serializable
data class TournamentView(val tournamentName: String): Screen {
    override val title: String = tournamentName
}
