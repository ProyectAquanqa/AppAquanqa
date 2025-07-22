package com.tecsup.aquanqa.data.network

/**
 * Excepción lanzada cuando el usuario no está registrado en el sistema
 * @param message Mensaje de error
 */
class UserNotFoundException(message: String = "Usuario no registrado") : Exception(message)

/**
 * Excepción lanzada cuando la contraseña proporcionada es incorrecta
 * @param message Mensaje de error
 */
class InvalidPasswordException(message: String = "Contraseña incorrecta") : Exception(message)

/**
 * Excepción genérica para errores de autenticación
 * @param message Mensaje de error
 */
class AuthenticationException(message: String) : Exception(message) 