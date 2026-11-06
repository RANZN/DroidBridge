package com.ranjan.droidbridge.presentation.help

import androidx.lifecycle.viewModelScope
import com.ranjan.droidbridge.domain.model.AppInfo
import com.ranjan.droidbridge.domain.model.UpdateCheck
import com.ranjan.droidbridge.domain.usecase.CheckForUpdateUseCase
import com.ranjan.droidbridge.presentation.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Help menu: About the developer and Check for Updates. */
class HelpViewModel(
    appInfo: AppInfo,
    private val checkForUpdate: CheckForUpdateUseCase,
) : MviViewModel<HelpState, HelpIntent, HelpEffect>(HelpState(appInfo)) {

    private var checkJob: Job? = null

    override fun onIntent(intent: HelpIntent) {
        when (intent) {
            HelpIntent.ShowAbout -> setState { copy(isAboutVisible = true) }
            HelpIntent.HideAbout -> setState { copy(isAboutVisible = false) }
            HelpIntent.CheckForUpdates -> check()
            HelpIntent.DismissUpdate -> {
                checkJob?.cancel()
                setState { copy(update = UpdateUiState.Hidden) }
            }
            is HelpIntent.OpenLink -> sendEffect(HelpEffect.OpenUrl(intent.url))
        }
    }

    private fun check() {
        if (currentState.update == UpdateUiState.Checking) return
        setState { copy(update = UpdateUiState.Checking) }
        checkJob = viewModelScope.launch {
            val result = checkForUpdate()
            setState {
                copy(
                    update = result.fold(
                        onSuccess = {
                            when (it) {
                                is UpdateCheck.UpToDate -> UpdateUiState.UpToDate(it.currentVersion)
                                is UpdateCheck.Available -> UpdateUiState.Available(it.release, it.currentVersion)
                            }
                        },
                        onFailure = { UpdateUiState.Failed(it.message ?: "Could not check for updates") },
                    )
                )
            }
        }
    }
}
