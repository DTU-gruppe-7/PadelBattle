package dk.dtu.padelbattle.presentation.common

/**
 * Generisk sealed class til at repræsentere UI-tilstand.
 * Bruges til at håndtere loading, success og error states konsistent på tværs af ViewModels.
 *
 * Eksempel:
 * ```kotlin
 * val uiState: StateFlow<UiState<Tournament>> = ...
 *
 * when (val state = uiState.collectAsState().value) {
 *     is UiState.Loading -> LoadingIndicator()
 *     is UiState.Success -> TournamentContent(state.data)
 *     is UiState.Error -> ErrorMessage(state.message)
 * }
 * ```
 */
sealed class UiState<out T> {
    
    /**
     * Indlæser data.
     */
    data object Loading : UiState<Nothing>()
    
    /**
     * Data indlæst succesfuldt.
     * @param data Den indlæste data
     */
    data class Success<T>(val data: T) : UiState<T>()
    
    /**
     * Fejl opstod under indlæsning.
     * @param message Fejlbesked til brugeren
     * @param exception Optional exception for logging
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : UiState<Nothing>()
    
    /**
     * Hjælpefunktioner til at arbejde med UiState.
     */
    companion object {
        /**
         * Mapper data i en Success state.
         */
        fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
            is Loading -> Loading
            is Success -> Success(transform(data))
            is Error -> Error(message, exception)
        }
        
        /**
         * Returnerer data hvis Success, ellers null.
         */
        fun <T> UiState<T>.getOrNull(): T? = when (this) {
            is Success -> data
            else -> null
        }
        
        /**
         * Returnerer data hvis Success, ellers default.
         */
        fun <T> UiState<T>.getOrDefault(default: T): T = when (this) {
            is Success -> data
            else -> default
        }
        
        /**
         * Tjekker om state er Loading.
         */
        val <T> UiState<T>.isLoading: Boolean get() = this is Loading
        
        /**
         * Tjekker om state er Success.
         */
        val <T> UiState<T>.isSuccess: Boolean get() = this is Success
        
        /**
         * Tjekker om state er Error.
         */
        val <T> UiState<T>.isError: Boolean get() = this is Error
    }
}
