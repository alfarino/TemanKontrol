package id.temankontrol.app.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>

    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure
    fun getOrNull(): T? = (this as? Success)?.data
}

sealed interface AppError {
    data class Validation(val field: String, val message: String) : AppError
    data object PriorityLimitExceeded : AppError
    data object Duplicate : AppError
    data object NotFound : AppError
    data object Storage : AppError
    data class BackupError(val message: String) : AppError
}
