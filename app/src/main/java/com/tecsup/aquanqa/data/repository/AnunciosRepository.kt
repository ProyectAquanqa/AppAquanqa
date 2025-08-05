package com.tecsup.aquanqa.data.repository

import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.model.content.Anuncio
import java.io.IOException

/**
 * Repositorio para gestionar los datos de los anuncios.
 *
 * Se encarga de obtener los anuncios desde la API y manejar los posibles errores de red,
 * devolviendo un resultado encapsulado (`Result<T>`) para que el ViewModel lo procese.
 */
class AnunciosRepository {

    /**
     * Obtiene la lista de anuncios filtrados por la categoría "Anuncios".
     *
     * @return Un objeto `Result` que contiene la lista de `Anuncio` si la llamada fue exitosa,
     * o un `IOException` si ocurrió un error.
     */
    suspend fun getAnuncios(): Result<List<Anuncio>> {
        return try {
            val response = ApiClient.apiService.getEventosPorCategoria("Anuncios")
            if (response.isSuccessful) {
                val anuncios = response.body()
                if (anuncios != null) {
                    Result.Success(anuncios)
                } else {
                    Result.Error(IOException("La respuesta de la API está vacía."))
                }
            } else {
                Result.Error(IOException("Error al obtener anuncios: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error de red al obtener anuncios.", e))
        }
    }
} 