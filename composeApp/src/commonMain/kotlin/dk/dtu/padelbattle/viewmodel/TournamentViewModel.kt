package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.model.TournamentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TournamentViewModel(
    private val repository: TournamentRepository
) : ViewModel() {

    private val _tournament = MutableStateFlow<Tournament?>(null)
    val tournament: StateFlow<Tournament?> = _tournament.asStateFlow()

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setTournament(tournament: Tournament) {
        _tournament.value = tournament
    }

    /**
     * Genindlæser turneringen fra databasen.
     * Bruges når nye kampe er blevet genereret og gemt i databasen.
     */
    fun reloadFromDatabase() {
        val currentId = _tournament.value?.id ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val reloadedTournament = repository.getTournamentById(currentId)
                _tournament.value = reloadedTournament
                _revision.value++
            } catch (e: Exception) {
                _error.value = "Kunne ikke genindlæse turnering: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTournament(updatedTournament: Tournament) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _tournament.value = updatedTournament
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sletter den aktuelle turnering.
     */
    fun deleteTournament(onSuccess: () -> Unit) {
        val currentId = _tournament.value?.id ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteTournament(currentId)
                _tournament.value = null
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Kunne ikke slette: ${e.message}"
            } finally {
                _isLoading.value = false
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

    fun clearError() {
        _error.value = null
    }

    /**
     * Opdaterer en spillers navn i den nuværende turnering og gemmer til databasen.
     */
    fun updatePlayerName(player: Player, newName: String) {
        val currentTournament = _tournament.value ?: return

        viewModelScope.launch {
            try {
                // 1. Find og erstat spilleren i players listen med ny kopi
                val playerIndex = currentTournament.players.indexOfFirst { it.id == player.id }
                if (playerIndex == -1) return@launch
                
                val updatedPlayer = currentTournament.players[playerIndex].copy(name = newName)
                currentTournament.players[playerIndex] = updatedPlayer

                // 2. Opdater alle matches der refererer til denne spiller
                currentTournament.matches.forEachIndexed { index, match ->
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
                    if (updatedMatch !== match) {
                        currentTournament.matches[index] = updatedMatch
                    }
                }

                // 3. Gem til database
                repository.updatePlayer(updatedPlayer, currentTournament.id)

                // 4. Trigger UI opdatering
                notifyTournamentUpdated()
            } catch (e: Exception) {
                _error.value = "Kunne ikke opdatere spillernavn: ${e.message}"
            }
        }
    }

    /**
     * Fortsætter en afsluttet turnering ved at generere en ny runde.
     */
    fun continueTournament(onSuccess: () -> Unit) {
        val currentTournament = _tournament.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Generer ny runde
                val success = currentTournament.extendTournament()

                if (!success) {
                    _error.value = "Kunne ikke generere ny runde"
                    return@launch
                }

                // 2. Find de nye kampe (dem der ikke er spillet endnu)
                val newMatches = currentTournament.matches.filter { !it.isPlayed }

                if (newMatches.isEmpty()) {
                    _error.value = "Ingen nye kampe blev genereret"
                    return@launch
                }

                // 3. Gem de nye kampe til databasen
                repository.insertMatches(newMatches, currentTournament.id)

                // 4. For Mexicano: Registrer at turneringen er udvidet (i databasen)
                if (currentTournament.type == TournamentType.MEXICANO) {
                    repository.registerExtension(currentTournament.id)
                }

                // 5. Marker turneringen som ikke-afsluttet
                currentTournament.isCompleted = false
                repository.updateTournamentCompleted(currentTournament.id, false)

                // 6. Opdater UI
                notifyTournamentUpdated()

                // 7. Kald success callback
                onSuccess()

            } catch (e: Exception) {
                _error.value = "Kunne ikke fortsætte turnering: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
