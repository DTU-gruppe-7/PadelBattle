package dk.dtu.padelbattle.presentation.tournament.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.domain.model.Player
import dk.dtu.padelbattle.domain.model.Tournament
import dk.dtu.padelbattle.domain.model.TournamentType
import dk.dtu.padelbattle.presentation.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TournamentViewModel(
    private val repository: TournamentRepository
) : ViewModel() {

    private val _tournament = MutableStateFlow<Tournament?>(null)
    val tournament: StateFlow<Tournament?> = _tournament.asStateFlow()

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    // UiState til asynkrone operationer
    private val _operationState = MutableStateFlow<UiState<OperationResult>>(UiState.Success(OperationResult.Idle))
    val operationState: StateFlow<UiState<OperationResult>> = _operationState.asStateFlow()

    /**
     * Resultat af asynkrone operationer i TournamentViewModel.
     */
    sealed class OperationResult {
        data object Idle : OperationResult()
        data object TournamentReloaded : OperationResult()
        data object TournamentDeleted : OperationResult()
        data object TournamentExtended : OperationResult()
        data object PlayerUpdated : OperationResult()
    }

    // Backwards-compatible derived properties fra operationState
    // Disse er reaktive og opdateres automatisk når operationState ændres
    val isLoading: StateFlow<Boolean> = _operationState
        .map { it is UiState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val error: StateFlow<String?> = _operationState
        .map { (it as? UiState.Error)?.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setTournament(tournament: Tournament) {
        _tournament.value = tournament
    }

    /**
     * Loader en turnering fra databasen baseret på ID og sætter den som aktuel.
     * Bruges ved navigation fra HomeScreen hvor kun summary er tilgængelig.
     * 
     * @param tournamentId ID på turneringen
     * @param onLoaded Callback med turneringens navn (til navigation)
     */
    fun loadTournamentById(tournamentId: String, onLoaded: (String?) -> Unit) {
        _operationState.value = UiState.Loading
        
        viewModelScope.launch {
            try {
                val tournament = repository.getTournamentById(tournamentId)
                if (tournament != null) {
                    _tournament.value = tournament
                    _operationState.value = UiState.Success(OperationResult.TournamentReloaded)
                    onLoaded(tournament.name)
                } else {
                    _operationState.value = UiState.Error("Turnering ikke fundet")
                    onLoaded(null)
                }
            } catch (e: Exception) {
                _operationState.value = UiState.Error("Kunne ikke loade turnering: ${e.message}", e)
                onLoaded(null)
            }
        }
    }

    /**
     * Genindlæser turneringen fra databasen.
     * Bruges når nye kampe er blevet genereret og gemt i databasen.
     * @param onComplete Optional callback der kaldes når genindlæsning er færdig
     */
    fun reloadFromDatabase(onComplete: ((Tournament?) -> Unit)? = null) {
        val currentId = _tournament.value?.id ?: return

        _operationState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val reloadedTournament = repository.getTournamentById(currentId)
                _tournament.value = reloadedTournament
                _revision.value++
                _operationState.value = UiState.Success(OperationResult.TournamentReloaded)
                onComplete?.invoke(reloadedTournament)
            } catch (e: Exception) {
                _operationState.value = UiState.Error("Kunne ikke genindlæse turnering: ${e.message}", e)
                onComplete?.invoke(null)
            }
        }
    }

    fun updateTournament(updatedTournament: Tournament) {
        _tournament.value = updatedTournament
    }

    /**
     * Sletter den aktuelle turnering.
     */
    fun deleteTournament(onSuccess: () -> Unit) {
        val currentId = _tournament.value?.id ?: return

        _operationState.value = UiState.Loading

        viewModelScope.launch {
            try {
                repository.deleteTournament(currentId)
                _tournament.value = null
                _operationState.value = UiState.Success(OperationResult.TournamentDeleted)
                onSuccess()
            } catch (e: Exception) {
                _operationState.value = UiState.Error("Kunne ikke slette: ${e.message}", e)
            }
        }
    }

    /**
     * Notificerer at turneringens data er blevet opdateret.
     * Trigger en recomposition af UI'et.
     */
    fun notifyTournamentUpdated() {
        _revision.value++
    }

    /**
     * Nulstiller operationState til idle.
     */
    fun clearOperationState() {
        _operationState.value = UiState.Success(OperationResult.Idle)
    }

    /**
     * Nulstiller al state i ViewModel.
     * Bruges når man navigerer væk fra en turnering for at undgå stale data.
     */
    fun reset() {
        _tournament.value = null
        _revision.value = 0
        _operationState.value = UiState.Success(OperationResult.Idle)
    }

    /**
     * Opdaterer en spillers navn i den nuværende turnering og gemmer til databasen.
     */
    fun updatePlayerName(player: Player, newName: String) {
        val currentTournament = _tournament.value ?: return

        _operationState.value = UiState.Loading

        viewModelScope.launch {
            try {
                // 1. Opret opdateret spiller
                val updatedPlayer = player.copy(name = newName)

                // 2. Opret ny players liste med opdateret spiller
                val updatedPlayers = currentTournament.players.map { p ->
                    if (p.id == player.id) updatedPlayer else p
                }

                // 3. Opdater alle matches der refererer til denne spiller
                val updatedMatches = currentTournament.matches.map { match ->
                    var updatedMatch = match
                    if (match.team1Player1.id == player.id) {
                        updatedMatch = updatedMatch.copy(team1Player1 = updatedPlayer)
                    }
                    if (match.team1Player2.id == player.id) {
                        updatedMatch = updatedMatch.copy(team1Player2 = updatedPlayer)
                    }
                    if (match.team2Player1.id == player.id) {
                        updatedMatch = updatedMatch.copy(team2Player1 = updatedPlayer)
                    }
                    if (match.team2Player2.id == player.id) {
                        updatedMatch = updatedMatch.copy(team2Player2 = updatedPlayer)
                    }
                    updatedMatch
                }

                // 4. Opret ny tournament med opdaterede lister
                val updatedTournament = currentTournament.copy(
                    players = updatedPlayers,
                    matches = updatedMatches
                )

                // 5. Gem til database
                repository.updatePlayer(updatedPlayer, currentTournament.id)

                // 6. Opdater lokal state
                _tournament.value = updatedTournament

                // 7. Trigger UI opdatering
                notifyTournamentUpdated()

                _operationState.value = UiState.Success(OperationResult.PlayerUpdated)
            } catch (e: Exception) {
                _operationState.value = UiState.Error("Kunne ikke opdatere spillernavn: ${e.message}", e)
            }
        }
    }

    /**
     * Fortsætter en afsluttet turnering ved at generere en ny runde.
     */
    fun continueTournament(onSuccess: () -> Unit) {
        val currentTournament = _tournament.value ?: return

        _operationState.value = UiState.Loading

        viewModelScope.launch {
            try {
                // 1. Generer ny runde (returnerer ny immutable Tournament)
                val extendedTournament = currentTournament.generateExtensionMatches()

                if (extendedTournament == null) {
                    _operationState.value = UiState.Error("Kunne ikke generere ny runde")
                    return@launch
                }

                // 2. Find de nye kampe (forskellen mellem gammel og ny)
                val existingMatchIds = currentTournament.matches.map { it.id }.toSet()
                val newMatches = extendedTournament.matches.filter { it.id !in existingMatchIds }

                if (newMatches.isEmpty()) {
                    _operationState.value = UiState.Error("Ingen nye kampe blev genereret")
                    return@launch
                }

                // 3. Gem de nye kampe til databasen
                repository.insertMatches(newMatches, currentTournament.id)

                // 4. For Mexicano: Registrer at turneringen er udvidet (i databasen)
                if (currentTournament.type == TournamentType.MEXICANO) {
                    repository.registerExtension(currentTournament.id)
                }

                // 5. Marker turneringen som ikke-afsluttet
                val finalTournament = extendedTournament.copy(isCompleted = false)
                repository.updateTournamentCompleted(currentTournament.id, false)

                // 6. Opdater lokal state
                _tournament.value = finalTournament

                // 7. Trigger UI opdatering
                notifyTournamentUpdated()

                _operationState.value = UiState.Success(OperationResult.TournamentExtended)

                // 8. Kald success callback
                onSuccess()

            } catch (e: Exception) {
                _operationState.value = UiState.Error("Kunne ikke fortsætte turnering: ${e.message}", e)
            }
        }
    }
}
