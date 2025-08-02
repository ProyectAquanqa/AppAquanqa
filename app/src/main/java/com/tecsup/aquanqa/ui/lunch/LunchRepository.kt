package com.tecsup.aquanqa.ui.lunch

import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiService
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first

/**
 * Repositorio para la gestión de almuerzos del comedor de Tecsup.
 *
 * Se encarga de la comunicación con la API para obtener los menús diarios de almuerzo,
 * filtrando automáticamente los días feriados para mostrar solo días laborables.
 * Este repositorio centraliza toda la lógica de datos relacionada con los almuerzos.
 *
 * @property apiService Servicio de API para realizar peticiones HTTP
 * @property userPreferences Preferencias del usuario para obtener tokens de autenticación
 */
class LunchRepository(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {

    /**
     * Obtiene la lista de almuerzos disponibles desde la API.
     *
     * Realiza una petición GET al endpoint `api/almuerzos/` con filtros aplicados:
     * - es_feriado=false: Excluye días feriados
     * - ordering=fecha: Ordena por fecha ascendente
     * 
     * Incluye el token de autenticación del usuario en la petición.
     * Maneja tanto respuestas exitosas como errores de red o de la API.
     *
     * @return `Result.Success(List<Almuerzo>)` si la petición es exitosa.
     * @return `Result.Error(Exception)` si ocurre algún error.
     */
    suspend fun getAlmuerzos(): Result<List<Almuerzo>> {
        return try {
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("Token de acceso no disponible"))
            }

            val response = apiService.getAlmuerzos(
                token = "Bearer $token",
                esFeriado = false, // Filtrar solo días laborables
                ordering = "fecha"  // Ordenar por fecha ascendente
            )
            
            if (response.isSuccessful && response.body() != null) {
                val almuerzos = response.body()!!
                
                // Log para debugging (se puede remover en producción)
                println("LunchRepository: Obtenidos ${almuerzos.size} almuerzos desde la API")
                
                Result.Success(almuerzos)
            } else {
                Result.Error(Exception("Error al obtener almuerzos: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Obtiene un almuerzo específico por su ID.
     * 
     * @param almuerzoId ID del almuerzo a obtener
     * @return Result con el almuerzo encontrado o error si no existe
     */
    suspend fun getAlmuerzoById(almuerzoId: Int): Almuerzo? {
        return try {
            when (val result = getAlmuerzos()) {
                is Result.Success -> {
                    result.data.find { it.id == almuerzoId }
                }
                is Result.Error -> null
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Verifica si hay almuerzos disponibles para la fecha actual o futuras.
     * 
     * @return true si hay almuerzos disponibles, false en caso contrario
     */
    suspend fun hasAvailableLunches(): Boolean {
        return when (val result = getAlmuerzos()) {
            is Result.Success -> result.data.isNotEmpty()
            is Result.Error -> false
            else -> false
        }
    }
}