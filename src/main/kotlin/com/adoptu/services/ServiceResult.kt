package com.adoptu.services

public sealed class ServiceResult<out T> {
    data class Success<T>(val data: T) : ServiceResult<T>()
    data object NotFound : ServiceResult<Nothing>()
    data object Forbidden : ServiceResult<Nothing>()
}
