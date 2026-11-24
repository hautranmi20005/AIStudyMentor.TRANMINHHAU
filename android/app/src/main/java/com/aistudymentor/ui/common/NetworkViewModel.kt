package com.aistudymentor.ui.common

import androidx.annotation.StringRes
import com.aistudymentor.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudymentor.data.repository.ApiFailure
import com.aistudymentor.data.repository.isRetryableNetworkFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException

data class SnackbarEvent(
    @StringRes val messageRes: Int,
    val messageArgs: List<Any> = emptyList(),
    @StringRes val retryLabelRes: Int? = null
)

abstract class NetworkViewModel : ViewModel() {
    private val mutableSnackbarEvents = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 1)
    val snackbarEvents = mutableSnackbarEvents.asSharedFlow()
    private var retryAction: (suspend () -> Unit)? = null

    protected fun launchRequest(
        setLoading: (Boolean) -> Unit,
        retry: suspend () -> Unit,
        action: suspend () -> Unit,
        showFailureMessage: Boolean = true,
        onFailure: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            setLoading(true)
            try {
                action()
                retryAction = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                onFailure(failure)
                val canRetry = failure.isRetryableNetworkFailure()
                retryAction = retry.takeIf { canRetry && showFailureMessage }
                if (showFailureMessage) {
                    mutableSnackbarEvents.emit(
                        SnackbarEvent(
                            messageRes = userFacingMessage(failure),
                            retryLabelRes = if (canRetry) R.string.retry else null
                        )
                    )
                }
            } finally {
                setLoading(false)
            }
        }
    }

    protected fun emitSnackbar(@StringRes messageRes: Int, vararg args: Any) {
        mutableSnackbarEvents.tryEmit(SnackbarEvent(messageRes, args.toList()))
    }

    fun retryLastAction() {
        val retry = retryAction ?: return
        retryAction = null
        viewModelScope.launch { retry() }
    }

    @StringRes
    private fun userFacingMessage(failure: Throwable): Int {
        val apiFailure = failure as? ApiFailure
        return when {
            apiFailure?.code == "ACCOUNT_PENDING_CONSENT" -> R.string.error_guardian_consent
            apiFailure?.code == "AI_QUOTA_EXCEEDED" -> R.string.error_quota_exceeded
            apiFailure?.code == "ACCOUNT_LOCKED" -> R.string.error_account_locked
            apiFailure?.code == "INVALID_REQUEST" -> R.string.error_invalid_request
            apiFailure?.code == "AI_PROVIDER_TIMEOUT" -> R.string.error_ai_timeout
            apiFailure?.code == "AI_PROVIDER_ERROR" -> R.string.error_ai_unavailable
            apiFailure?.code == "AI_NOT_CONFIGURED" -> R.string.error_ai_not_configured
            apiFailure?.statusCode == 401 -> R.string.error_session_expired
            apiFailure?.statusCode == 403 -> R.string.error_permission_denied
            apiFailure?.statusCode == 404 -> R.string.error_not_found
            apiFailure?.statusCode == 429 -> R.string.error_rate_limited
            failure is IOException -> R.string.error_network
            else -> R.string.error_generic
        }
    }
}