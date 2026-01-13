package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.dao.MatchDao
import dk.dtu.padelbattle.data.dao.PlayerDao
import dk.dtu.padelbattle.data.dao.TournamentDao
import dk.dtu.padelbattle.data.mapper.loadFullTournamentFromDao
import dk.dtu.padelbattle.data.mapper.toEntity
import dk.dtu.padelbattle.data.entity.PlayerEntity
import dk.dtu.padelbattle.model.MexicanoExtensionTracker
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.model.TournamentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TournamentViewModel(
    private val tournamentDao: TournamentDao,
    private val playerDao: PlayerDao,
    private val matchDao: MatchDao
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
                val reloadedTournament = loadFullTournamentFromDao(
                    currentId,
                    tournamentDao,
                    playerDao,
                    matchDao
                )
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
     * Notificerer at turneringens data er blevet opdateret (f.eks. kampresultater).
     * Dette trigger en recomposition af UI'et ved at inkrementere revision counter.
     */
    fun deleteTournament(onSuccess: () -> Unit) {
        val currentId = _tournament.value?.id ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Slet fra DB (Antager du har mapper funktionen eller kalder dao direkte)
                dk.dtu.padelbattle.data.mapper.deleteTournamentFromDao(currentId, tournamentDao)

                _tournament.value = null
                onSuccess() // Naviger væk
            } catch (e: Exception) {
                _error.value = "Kunne ikke slette: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun notifyTournamentUpdated() {
        _revision.value++
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Opdaterer en spillers navn i den nuværende turnering og gemmer til databasen.
     * Navnet opdateres både i tournament.players listen og i matches hvor spilleren deltager.
     */
    fun updatePlayerName(player: Player, newName: String) {
        val currentTournament = _tournament.value ?: return
        
        viewModelScope.launch {
            try {
                // 1. Opdater spilleren i players listen
                val playerInList = currentTournament.players.find { it.id == player.id }
                playerInList?.name = newName
                
                // 2. Opdater spilleren i alle matches
                currentTournament.matches.forEach { match ->
                    if (match.team1Player1.id == player.id) match.team1Player1.name = newName
                    if (match.team1Player2.id == player.id) match.team1Player2.name = newName
                    if (match.team2Player1.id == player.id) match.team2Player1.name = newName
                    if (match.team2Player2.id == player.id) match.team2Player2.name = newName
                }
                
                // 3. Gem til database
                playerInList?.let { p ->
                    playerDao.updatePlayer(
                        PlayerEntity(
                            id = p.id,
                            tournamentId = currentTournament.id,
                            name = p.name,
                            totalPoints = p.totalPoints,
                            gamesPlayed = p.gamesPlayed,
                            wins = p.wins,
                            losses = p.losses,
                            draws = p.draws
                        )
                    )
                }
                
                // 4. Trigger UI opdatering
                notifyTournamentUpdated()
            } catch (e: Exception) {
                _error.value = "Kunne ikke opdatere spillernavn: ${e.message}"
            }
        }
    }

    /**
     * Fortsætter en afsluttet turnering ved at generere en ny runde.
     * Markerer turneringen som ikke-afsluttet og gemmer de nye kampe til databasen.
     * @param onSuccess Callback der kaldes når operationen er færdig - navigér til kampe-tab
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
                matchDao.insertMatches(newMatches.map { it.toEntity(currentTournament.id) })
                
                // 4. For Mexicano: Registrer at turneringen er udvidet (kræver 2 runder mere)
                if (currentTournament.type == TournamentType.MEXICANO) {
                    MexicanoExtensionTracker.registerExtension(currentTournament.id)
                }
                
                // 5. Marker turneringen som ikke-afsluttet
                currentTournament.isCompleted = false
                tournamentDao.updateTournamentCompleted(currentTournament.id, false)
                
                // 5. Opdater UI
                notifyTournamentUpdated()
                
                // 6. Kald success callback
                onSuccess()
                
            } catch (e: Exception) {
                _error.value = "Kunne ikke fortsætte turnering: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}


