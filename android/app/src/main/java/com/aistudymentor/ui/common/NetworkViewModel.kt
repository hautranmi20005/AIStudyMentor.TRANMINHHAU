package com.aistudymentor.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudymentor.data.repository.ApiFailure
import com.aistudymentor.data.repository.isRetryableNetworkFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException

data class SnackbarEvent(val message: String, val retryLabel: String? = null)

abstract class NetworkViewModel : ViewModel() {
    private val mutableSnackbarEvents = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 1)
    val snackbarEvents = mutableSnackbarEvents.asSharedFlow()
    private var retryAction: (suspend () -> Unit)? = null

    protected fun launchRequest(
        setLoading: (Boolean) -> Unit,
        retry: suspend () -> Unit,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            setLoading(true)
            try {
                action()
                retryAction = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val canRetry = failure.isRetryableNetworkFailure()
                retryAction = retry.takeIf { canRetry }
                mutableSnackbarEvents.emit(
                    SnackbarEvent(
                        message = userFacingMessage(failure),
                        retryLabel = if (canRetry) "Retry" else null
                    )
                )
            } finally {
                setLoading(false)
            }
        }
    }

    protected fun emitSnackbar(message: String) {
        mutableSnackbarEvents.tryEmit(SnackbarEvent(message))
    }

    fun retryLastAction() {
        val retry = retryAction ?: return
        retryAction = null
        viewModelScope.launch { retry() }
    }

    private fun userFacingMessage(failure: Throwable): String = when {
        (failure as? ApiFailure)?.code == "ACCOUNT_PENDING_CONSENT" ->
            "Guardian approval is required before using study features."
        failure is ApiFailure -> failure.message
        failure is IOException -> "Network connection problem. Check your connection and try again."
        else -> "Something went wrong. Please try again."
    }
}