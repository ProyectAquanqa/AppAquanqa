package com.tecsup.aquanqa.data.repository

import android.util.Log
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiService
import com.tecsup.aquanqa.data.model.content.Almuerzo
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    
    companion object {
        private const val TAG = "LunchRepository"
    }
    
    private val gson = Gson()

    /**
     * Obtiene la lista de almuerzos con cache persistente inteligente.
     * 
     * Estrategia de cache:
     * 1. Verifica cache en DataStore (persiste al cerrar app)
     * 2. Si cache válido → devuelve inmediatamente
     * 3. Si cache expirado → llama API
     * 4. Si API falla → usa cache expirado como fallback
     * 5. Si API funciona → actualiza cache y devuelve datos frescos
     *
     * @param forceRefresh Si es true, ignora cache y llama API directamente
     * @return Result.Success con lista de almuerzos o Result.Error
     */
    suspend fun getAlmuerzos(forceRefresh: Boolean = false): Result<List<Almuerzo>> {
        return try {
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("Token de acceso no disponible"))
            }

            // 1. VERIFICAR CACHE PERSISTENTE (si no es refresh forzado)
            if (!forceRefresh) {
                val cachedAlmuerzosJson = userPreferences.cachedAlmuerzos.first()
                val isCacheExpired = userPreferences.isAlmuerzosCacheExpired()
                
                if (cachedAlmuerzosJson != null && !isCacheExpired) {
                    // Cache válido - devolver inmediatamente
                    val cachedAlmuerzos = deserializeAlmuerzos(cachedAlmuerzosJson)
                    if (cachedAlmuerzos != null) {
                        Log.d(TAG, "Almuerzos from valid cache: ${cachedAlmuerzos.size} items")
                        return Result.Success(cachedAlmuerzos)
                    }
                }
                
                // Cache expirado pero disponible para fallback
                if (cachedAlmuerzosJson != null && isCacheExpired) {
                    Log.d(TAG, "Almuerzos cache expired, fetching fresh data")
                }
            }

            // 2. LLAMAR API PARA DATOS FRESCOS
            val response = apiService.getAlmuerzos(
                token = "Bearer $token",
                esFeriado = false, // Filtrar solo días laborables
                ordering = "fecha"  // Ordenar por fecha ascendente
            )
            
            if (response.isSuccessful && response.body() != null) {
                val almuerzos = response.body()!!
                
                // 3. GUARDAR EN CACHE PERSISTENTE
                val almuerzosSerialized = serializeAlmuerzos(almuerzos)
                userPreferences.saveAlmuerzos(almuerzosSerialized)
                
                Log.d(TAG, "Almuerzos updated from API and cached: ${almuerzos.size} items")
                Result.Success(almuerzos)
            } else {
                // 4. API FALLÓ - USAR CACHE EXPIRADO COMO FALLBACK
                val cachedAlmuerzosJson = userPreferences.cachedAlmuerzos.first()
                if (cachedAlmuerzosJson != null) {
                    val cachedAlmuerzos = deserializeAlmuerzos(cachedAlmuerzosJson)
                    if (cachedAlmuerzos != null) {
                        Log.w(TAG, "API failed, using expired cache as fallback: ${cachedAlmuerzos.size} items")
                        return Result.Success(cachedAlmuerzos)
                    }
                }
                
                Result.Error(Exception("Error al obtener almuerzos: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            // 5. ERROR DE RED - USAR CACHE EXPIRADO COMO ÚLTIMO RECURSO
            try {
                val cachedAlmuerzosJson = userPreferences.cachedAlmuerzos.first()
                if (cachedAlmuerzosJson != null) {
                    val cachedAlmuerzos = deserializeAlmuerzos(cachedAlmuerzosJson)
                    if (cachedAlmuerzos != null) {
                        Log.w(TAG, "Network error, using expired cache as fallback: ${e.message}")
                        return Result.Success(cachedAlmuerzos)
                    }
                }
            } catch (cacheException: Exception) {
                Log.e(TAG, "Error accessing cache: ${cacheException.message}")
            }
            
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

    /**
     * Verifica si debe hacer refresh automático basado en expiración de cache.
     */
    suspend fun shouldAutoRefresh(): Boolean {
        return try {
            userPreferences.isAlmuerzosCacheExpired()
        } catch (e: Exception) {
            true // En caso de error, hacer refresh
        }
    }

    /**
     * Refresca los almuerzos forzando una llamada a la API.
     * SIEMPRE obtiene datos frescos del servidor.
     */
    suspend fun refreshAlmuerzos(): Result<List<Almuerzo>> {
        Log.d(TAG, "Force refreshing almuerzos from API")
        return getAlmuerzos(forceRefresh = true)
    }

    /**
     * Limpia el cache de almuerzos.
     * Útil para debugging o cuando se necesita limpiar datos obsoletos.
     */
    suspend fun clearCache() {
        userPreferences.clearAlmuerzosCache()
        Log.d(TAG, "Almuerzos cache cleared")
    }

    // ================= MÉTODOS PRIVADOS DE SERIALIZACIÓN =================

    /**
     * Serializa la lista de almuerzos a JSON para guardar en DataStore.
     */
    private fun serializeAlmuerzos(almuerzos: List<Almuerzo>): String {
        return try {
            gson.toJson(almuerzos)
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing almuerzos: ${e.message}")
            "[]" // JSON vacío como fallback
        }
    }

    /**
     * Deserializa el JSON de almuerzos desde DataStore.
     */
    private fun deserializeAlmuerzos(json: String): List<Almuerzo>? {
        return try {
            val type = object : TypeToken<List<Almuerzo>>() {}.type
            gson.fromJson<List<Almuerzo>>(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing almuerzos: ${e.message}")
            null
        }
    }
}