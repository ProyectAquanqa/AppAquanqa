package com.tecsup.aquanqa.data.network

/**
 * Excepción lanzada cuando el usuario no está registrado en el sistema
 */
class UserNotFoundException(message: String = "Usuario no registrado") : Exception(message)

/**
 * Excepción lanzada cuando la contraseña proporcionada es incorrecta
 */
class InvalidPasswordException(message: String = "Contraseña incorrecta") : Exception(message)

/**
 * Excepción genérica para errores de autenticación
 */
class AuthenticationException(message: String) : Exception(message)
/**
 * Excepción lanzada cuando no hay conexión a internet o hay errores de red
 */
class NetworkException(message: String = "No se pudo conectar con el servidor. Verifica tu conexión a internet o inténtalo más tarde.") : Exception(message)

/**
 * Excepción lanzada cuando el servidor no está disponible o hay timeout
 */
class ServerUnavailableException(message: String = "El servidor no está disponible en este momento. Inténtalo más tarde.") : Exception(message) 