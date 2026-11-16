package com.zara.friend.core.common

/**
 * Pembungkus hasil operasi yang bisa gagal (panggilan Gemini, sync, dsb).
 * Menghindari try-catch tersebar di ViewModel/use case.
 */
sealed interface AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>

    data class Error(val throwable: Throwable) : AppResult<Nothing>

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Error

    /** Ambil data bila sukses, atau null bila gagal. */
    fun getOrNull(): T? = (this as? Success)?.data

    /** Ambil data bila sukses, atau [default] bila gagal. */
    fun getOrDefault(default: @UnsafeVariance T): T = getOrNull() ?: default

    /** Transformasi nilai bila sukses. */
    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    companion object {
        /** Jalankan [block], tangkap exception menjadi [Error]. */
        inline fun <T> runCatching(block: () -> T): AppResult<T> = try {
            Success(block())
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Error(t)
        }
    }
}
