package dk.dtu.padelbattle.presentation.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fælles handler til bekræftelsesdialog for sletning.
 * Bruges af både HomeViewModel og SettingsViewModel for at undgå duplikering.
 */
class DeleteConfirmationHandler {
    
    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation.asStateFlow()

    private var onConfirmAction: (() -> Unit)? = null

    /**
     * Viser bekræftelsesdialog og gemmer handlingen der skal udføres ved bekræftelse.
     */
    fun show(onConfirm: () -> Unit) {
        onConfirmAction = onConfirm
        _showDeleteConfirmation.value = true
    }

    /**
     * Lukker dialogen uden at udføre handlingen.
     */
    fun dismiss() {
        _showDeleteConfirmation.value = false
        onConfirmAction = null
    }

    /**
     * Bekræfter og udfører den gemte handling.
     */
    fun confirm() {
        onConfirmAction?.invoke()
        dismiss()
    }
}
