package com.tecsup.aquanqa.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.cache.CacheManager
import com.tecsup.aquanqa.data.cache.CacheResult
import com.tecsup.aquanqa.data.model.content.Anuncio
import com.tecsup.aquanqa.data.model.content.EventoBasico
import com.tecsup.aquanqa.data.model.content.toEventoBasico
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * Repositorio refactorizado para anuncios con cache híbrido inteligente.
 * Memoria (rápido) + DataStore fallback (persistente) + imagen por defecto.
 */
class AnunciosRepository(
    private val userPreferences: UserPreferences,
    private val cacheManager: CacheManager = CacheManager()
) {
    
    companion object {
        private const val TAG = "AnunciosRepository"
        private const val ANUNCIOS_CACHE_KEY = "anuncios_category"
    }
    
    private val gson = Gson()

    /**
     * Obtiene anuncios con cache híbrido inteligente.
     * Memoria (rápido) + DataStore fallback (persistente) + imagen por defecto.
     * 
     * @param forceRefresh Forzar actualización desde API
     */
    suspend fun getAnuncios(forceRefresh: Boolean = false): Result<List<Anuncio>> {
        return try {
            val userId = getCurrentUserId()
            
            // 1. VERIFICAR CACHE DE MEMORIA (si no es refresh forzado)
            if (!forceRefresh) {
                val memoryCacheResult = cacheManager.getCachedEventsByCategory(ANUNCIOS_CACHE_KEY, userId)
                
                when (memoryCacheResult) {
                    is CacheResult.Hit -> {
                        Log.d(TAG, "Anuncios from memory cache: ${memoryCacheResult.data.size} items")
                        return Result.Success(memoryCacheResult.data)
                    }
                    is CacheResult.Miss -> {
                        // Cache de memoria vacío, intentar fallback de DataStore
                        val fallbackResult = getAnunciosFallbackFromDataStore()
                        if (fallbackResult != null) {
                            Log.d(TAG, "Anuncios from DataStore fallback: ${fallbackResult.size} items")
                            return Result.Success(fallbackResult)
                        }
                    }
                    else -> {
                        Log.d(TAG, "Memory cache expired, fetching from API")
                    }
                }
            }
            
            // 2. LLAMAR API PARA DATOS FRESCOS (solo categoría "Anuncios")
            val token = userPreferences.accessToken.first()
            if (token.isNullOrEmpty()) {
                return Result.Error(IllegalStateException("Token de acceso no disponible"))
            }
            val response = ApiClient.apiService.getEventosAndroid(
                token = "Bearer $token",
                categoriaNombre = "Anuncios",
                ordering = "-is_pinned,-fecha"
            )
            
            if (response.isSuccessful) {
                val anuncios = response.body()
                if (anuncios != null) {
                    // 3. GUARDAR EN CACHE HÍBRIDO
                    // Cache de memoria (rápido)
                    cacheManager.cacheEventsByCategory(ANUNCIOS_CACHE_KEY, anuncios, userId)
                    
                    // Fallback en DataStore (persistente)
                    saveAnunciosFallbackToDataStore(anuncios)
                    
                    Log.d(TAG, "Anuncios updated from API with hybrid cache: ${anuncios.size} items")
                    Result.Success(anuncios)
                } else {
                    // API devolvió null, intentar fallback
                    val fallbackResult = getAnunciosFallbackFromDataStore()
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
                val memoryCacheResult = cacheManager.getCachedEventsByCategory(ANUNCIOS_CACHE_KEY, userId)
                
                when (memoryCacheResult) {
                    is CacheResult.Expired -> {
                        Log.w(TAG, "API failed, using expired memory cache as fallback")
                        return Result.Success(memoryCacheResult.data)
                    }
                    else -> {
                        // Intentar fallback de DataStore
                        val fallbackResult = getAnunciosFallbackFromDataStore()
                        if (fallbackResult != null) {
                            Log.w(TAG, "API failed, using DataStore fallback: ${fallbackResult.size} items")
                            return Result.Success(fallbackResult)
                        }
                    }
                }
                
                Result.Error(IOException("No se pudieron cargar los anuncios. Intenta nuevamente"))
            }
        } catch (e: Exception) {
            // 5. ERROR DE RED - USAR TODOS LOS FALLBACKS DISPONIBLES
            Log.w(TAG, "Network error, trying all fallbacks: ${e.message}")
            
            // Intentar fallback de DataStore
            val fallbackResult = getAnunciosFallbackFromDataStore()
            if (fallbackResult != null) {
                Log.w(TAG, "Network error, using DataStore fallback: ${fallbackResult.size} items")
                return Result.Success(fallbackResult)
            }
            
            Result.Error(IOException("Error de red al obtener anuncios.", e))
        }
    }

    /**
     * Obtiene el detalle de un evento por ID desde la API.
     * Usa cache híbrido opcionalmente a futuro; por ahora directa a API.
     */
    suspend fun getEventoById(id: Int): Result<Anuncio> {
        return try {
            val token = userPreferences.accessToken.first()
            if (token.isNullOrEmpty()) {
                return Result.Error(IllegalStateException("Token de acceso no disponible"))
            }

            val response = ApiClient.apiService.getEventoById("Bearer $token", id)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(IOException("No se pudo obtener el evento"))
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error de red al obtener evento", e))
        }
    }

    /**
     * Refresca anuncios forzando llamada a la API.
     * SIEMPRE obtiene datos frescos del servidor.
     */
    suspend fun refreshAnuncios(): Result<List<Anuncio>> {
        Log.d(TAG, "Force refreshing anuncios from API")
        return getAnuncios(forceRefresh = true)
    }

    /**
     * Limpia el cache de anuncios.
     */
    suspend fun clearCache() {
        val userId = getCurrentUserId()
        cacheManager.invalidateEvents(userId)
        userPreferences.clearAnunciosFallbackCache()
        Log.d(TAG, "Anuncios cache cleared")
    }

    //metodos privados

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
     * Guarda fallback de anuncios en DataStore.
     */
    private suspend fun saveAnunciosFallbackToDataStore(anuncios: List<Anuncio>) {
        try {
            val anunciosBasicos = anuncios.map { it.toEventoBasico() }
            val json = gson.toJson(anunciosBasicos)
            userPreferences.saveAnunciosFallback(json)
            Log.d(TAG, "Anuncios fallback saved to DataStore: ${anunciosBasicos.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving anuncios fallback: ${e.message}")
        }
    }

    /**
     * Obtiene fallback de anuncios desde DataStore.
     */
    private suspend fun getAnunciosFallbackFromDataStore(): List<Anuncio>? {
        return try {
            val fallbackJson = userPreferences.cachedAnunciosFallback.first()
            val isExpired = userPreferences.isAnunciosFallbackExpired()
            
            if (fallbackJson != null && !isExpired) {
                val type = object : TypeToken<List<EventoBasico>>() {}.type
                val anunciosBasicos = gson.fromJson<List<EventoBasico>>(fallbackJson, type)
                val anuncios = anunciosBasicos.map { it.toAnuncio() }
                Log.d(TAG, "Anuncios fallback loaded from DataStore: ${anuncios.size} items")
                anuncios
            } else {
                Log.d(TAG, "Anuncios fallback expired or not available")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading anuncios fallback: ${e.message}")
            null
        }
    }
} 