package dk.dtu.padelbattle.view.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    val title: String
}

@Serializable
object Home: Screen {
    override val title = "Padel Battle"
}

@Serializable
object ChoosePlayer: Screen {
    override val title = "Vælg Spillere"
}

@Serializable
object ChooseTournament: Screen {
    override val title = "Vælg Turnering"
}

@Serializable
data class TournamentConfig(val tournamentType: String): Screen {
    override val title = "Opsæt Turnering"
}

@Serializable
object TournamentView: Screen {
    override val title = "Turnering"
}

@Serializable
data class Gameplay(val playerNames: String): Screen {
    override val title = "Kamp"
}
