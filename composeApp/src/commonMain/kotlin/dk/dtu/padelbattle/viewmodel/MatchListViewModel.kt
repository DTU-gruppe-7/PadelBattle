package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import dk.dtu.padelbattle.model.Match
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel til at håndtere kamplisten.
 * Holder styr på kampe og trigger recomposition når kampe opdateres.
 */
class MatchListViewModel : ViewModel() {

    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches.asStateFlow()

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    /**
     * Sætter listen af kampe.
     * Opdaterer state, hvilket automatisk trigger recomposition.
     * Laver en ny liste-instans for at sikre at StateFlow opdager ændringer.
     */
    fun setMatches(matches: List<Match>) {
        _matches.value = matches.toList()
        _revision.value++
    }

    /**
     * Notificerer at en kamp er opdateret.
     * Bruges når Match objekter ændres in-place.
     */
    fun notifyMatchUpdated() {
        _revision.value++
    }
}

