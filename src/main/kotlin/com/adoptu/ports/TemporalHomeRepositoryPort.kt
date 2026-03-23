package com.adoptu.ports

import com.adoptu.dto.*

interface TemporalHomeRepositoryPort {
    fun getTemporalHome(userId: Int): TemporalHomeDto?
    fun createTemporalHome(userId: Int, request: CreateTemporalHomeRequest): TemporalHomeDto
    fun updateTemporalHome(userId: Int, request: UpdateTemporalHomeRequest): TemporalHomeDto?
    fun searchTemporalHomes(params: TemporalHomeSearchParams): List<TemporalHomeDto>
    fun createTemporalHomeRequest(temporalHomeId: Int, rescuerId: Int, petId: Int?, message: String): Int
    fun isBlocked(temporalHomeId: Int, rescuerId: Int): Boolean
    fun blockRescuer(temporalHomeId: Int, rescuerId: Int): Boolean
    fun getMyRequests(userId: Int): List<TemporalHomeRequestDto>
}
