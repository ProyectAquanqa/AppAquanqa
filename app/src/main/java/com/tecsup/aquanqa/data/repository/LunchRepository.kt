package com.tecsup.aquanqa.data.repository

import android.util.Log
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiService
import com.tecsup.aquanqa.data.cache.CacheManager
import com.tecsup.aquanqa.data.cache.CacheResult
import com.tecsup.aquanqa.data.model.content.Almuerzo
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.utils.ErrorHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * Repositorio optimizado para gestión de almuerzos con cache híbrido inteligente.
 * Memoria (rápido) + DataStore fallback (persistente) para máxima disponibilidad.
 */
class LunchRepository(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences,
    private val cacheManager: CacheManager = CacheManager()
) {
    
    companion object {
        private const val TAG = "LunchRepository"
        private const val ALMUERZOS_CACHE_KEY = "almuerzos_data"
    }
    
    private val gson = Gson()

    /**
     * Obtiene almuerzos con cache híbrido inteligente.
     * Memoria (rápido) + DataStore fallback (persistente) para máxima disponibilidad.
     * 
     * @param forceRefresh Forzar actualización desde API
     */
    suspend fun getAlmuerzos(forceRefresh: Boolean = false): Result<List<Almuerzo>> {
        return try {
            val userId = getCurrentUserId()
            
            // 1. VERIFICAR CACHE DE MEMORIA (si no es refresh forzado)
            if (!forceRefresh) {
                val memoryCacheResult = cacheManager.getCachedAlmuerzos(ALMUERZOS_CACHE_KEY, userId)
                
                when (memoryCacheResult) {
                    is CacheResult.Hit -> {
                        Log.d(TAG, "Almuerzos from memory cache: ${memoryCacheResult.data.size} items")
                        return Result.Success(memoryCacheResult.data)
                    }
                    is CacheResult.Miss -> {
                        // Cache de memoria vacío, intentar fallback de DataStore
                        val fallbackResult = getAlmuerzosFallbackFromDataStore()
                        if (fallbackResult != null) {
                            Log.d(TAG, "Almuerzos from DataStore fallback: ${fallbackResult.size} items")
                            return Result.Success(fallbackResult)
                        }
                    }
                    else -> {
                        Log.d(TAG, "Memory cache expired, fetching from API")
                    }
                }
            }
            
            // 2. LLAMAR API PARA DATOS FRESCOS
            val token = getValidToken() ?: return Result.Error(
                Exception("Su sesión ha expirado. Inicie sesión nuevamente.")
            )
            
            val response = apiService.getAlmuerzos(
                token = "Bearer $token",
                esFeriado = false,
                active = true,
                ordering = "fecha"
            )
            
            if (response.isSuccessful) {
                val almuerzos = response.body()
                if (almuerzos != null) {
                    // 3. GUARDAR EN CACHE HÍBRIDO
                    // Cache de memoria (rápido)
                    cacheManager.cacheAlmuerzos(ALMUERZOS_CACHE_KEY, almuerzos, userId)
                    
                    // Fallback en DataStore (persistente)
                    saveAlmuerzosFallbackToDataStore(almuerzos)
                    
                    Log.d(TAG, "Almuerzos updated from API with hybrid cache: ${almuerzos.size} items")
                    Result.Success(almuerzos)
                } else {
                    // API devolvió null, intentar fallback
                    val fallbackResult = getAlmuerzosFallbackFromDataStore()
                    if (fallbackResult != null) {
                        Log.w(TAG, "API returned null, using DataStore fallback: ${fallbackResult.size} items")
                        Result.Success(fallbackResult)
                    } else {
                        Result.Error(IOException("La respuesta de la API está vacía."))
                    }
                }
            } else {
                // 4. API FALLÓ - USAR FALLBACKS
                // Primero intentar cache de memoria expirado
                val memoryCacheResult = cacheManager.getCachedAlmuerzos(ALMUERZOS_CACHE_KEY, userId)
                
                when (memoryCacheResult) {
                    is CacheResult.Expired -> {
                        Log.w(TAG, "API failed, using expired memory cache as fallback")
                        return Result.Success(memoryCacheResult.data)
                    }
                    else -> {
                        // Intentar fallback de DataStore
                        val fallbackResult = getAlmuerzosFallbackFromDataStore()
                        if (fallbackResult != null) {
                            Log.w(TAG, "API failed, using DataStore fallback: ${fallbackResult.size} items")
                            return Result.Success(fallbackResult)
                        }
                    }
                }
                
                Result.Error(IOException("No se pudieron cargar los almuerzos. Intenta nuevamente"))
            }
        } catch (e: Exception) {
            // 5. ERROR DE RED - USAR TODOS LOS FALLBACKS DISPONIBLES
            Log.w(TAG, "Network error, trying all fallbacks: ${e.message}")
            
            // Intentar fallback de DataStore
            val fallbackResult = getAlmuerzosFallbackFromDataStore()
            if (fallbackResult != null) {
                Log.w(TAG, "Network error, using DataStore fallback: ${fallbackResult.size} items")
                return Result.Success(fallbackResult)
            }
            
            Result.Error(IOException("Error de red al obtener almuerzos.", e))
        }
    }

    /**
     * Obtiene un almuerzo específico por ID.
     */
    suspend fun getAlmuerzoById(almuerzoId: Int): Almuerzo? {
        return when (val result = getAlmuerzos()) {
            is Result.Success -> result.data.find { it.id == almuerzoId }
            else -> null
        }
    }

    /**
     * Verifica si hay almuerzos disponibles.
     */
    suspend fun hasAvailableLunches(): Boolean {
        return when (val result = getAlmuerzos()) {
            is Result.Success -> result.data.isNotEmpty()
            else -> false
        }
    }

    /**
     * Refresca datos forzando llamada a API.
     */
    suspend fun refreshAlmuerzos(): Result<List<Almuerzo>> {
        Log.d(TAG, "Refrescando almuerzos desde API")
        return getAlmuerzos(forceRefresh = true)
    }

    /**
     * Limpia el cache híbrido de almuerzos.
     */
    suspend fun clearCache() {
        val userId = getCurrentUserId()
        cacheManager.invalidateAlmuerzos(userId)
        userPreferences.clearAlmuerzosCache()
        Log.d(TAG, "Almuerzos cache cleared")
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Obtiene el ID del usuario actual.
     */
    private suspend fun getCurrentUserId(): String {
        return try {
            userPreferences.userDni.first() ?: "anonymous"
        } catch (e: Exception) {
            Log.w(TAG, "Could not get user ID, using anonymous: ${e.message}")
            "anonymous"
        }
    }

    /**
     * Obtiene token válido o null si no está disponible.
     */
    private suspend fun getValidToken(): String? {
        return userPreferences.accessToken.first()
    }

    /**
     * Guarda fallback de almuerzos en DataStore.
     */
    private suspend fun saveAlmuerzosFallbackToDataStore(almuerzos: List<Almuerzo>) {
        try {
            val json = gson.toJson(almuerzos)
            userPreferences.saveAlmuerzos(json)
            Log.d(TAG, "Almuerzos fallback saved to DataStore: ${almuerzos.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving almuerzos fallback: ${e.message}")
        }
    }

    /**
     * Obtiene fallback de almuerzos desde DataStore.
     */
    private suspend fun getAlmuerzosFallbackFromDataStore(): List<Almuerzo>? {
        return try {
            val fallbackJson = userPreferences.cachedAlmuerzos.first()
            val isExpired = userPreferences.isAlmuerzosCacheExpired()
            
            if (fallbackJson != null && !isExpired) {
                val almuerzos = deserializeAlmuerzos(fallbackJson)
                Log.d(TAG, "Almuerzos fallback loaded from DataStore: ${almuerzos?.size} items")
                almuerzos
            } else {
                Log.d(TAG, "Almuerzos fallback expired or not available")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading almuerzos fallback: ${e.message}")
            null
        }
    }

    /**
     * Deserializa almuerzos desde JSON de manera segura.
     */
    private fun deserializeAlmuerzos(json: String): List<Almuerzo>? {
        return try {
            val type = object : TypeToken<List<Almuerzo>>() {}.type
            gson.fromJson<List<Almuerzo>>(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializando almuerzos: ${e.message}")
            null
        }
    }
}