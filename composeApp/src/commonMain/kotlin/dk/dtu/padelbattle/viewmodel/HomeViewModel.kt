package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.dao.TournamentDao
import dk.dtu.padelbattle.data.entity.TournamentEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class HomeViewModel(
    private val tournamentDao: TournamentDao
) : ViewModel() {

    // Vi henter turneringer som et Flow og konverterer til StateFlow til UI'en.
    // Vi bruger stateIn for at holde flowet aktivt, så længe UI'en lever.
    val tournaments: StateFlow<List<TournamentEntity>> = tournamentDao.getAllTournaments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createDummyTournament() {
        viewModelScope.launch {
            val dummyId = "dummy_${Clock.System.now().toEpochMilliseconds()}"
            val dummyTournament = TournamentEntity(
                id = dummyId,
                name = "Hygge Padel ${ (1..100).random() }", // Tilfældigt navn
                type = "AMERICANO",
                dateCreated = Clock.System.now().toEpochMilliseconds(),
                isCompleted = true
            )

            tournamentDao.insertTournament(dummyTournament)
        }
    }
}