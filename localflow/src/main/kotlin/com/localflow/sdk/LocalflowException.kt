package com.localflow.sdk

/**
 * Base sealed class for all Locaflow SDK exceptions.
 */
sealed class LocalflowException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    class NotInitializedException : LocalflowException(
        "Localflow SDK is not initialized. Please call Localflow.initialize(context, config) first."
    )
    
    class InvalidApiKeyException(message: String) : LocalflowException(message)
    
    class NetworkException(message: String, cause: Throwable? = null) : LocalflowException(message, cause)
    
    class NoPublishedVersionException : LocalflowException(
        "The project has no published localization versions yet."
    )
    
    class LanguageNotFoundException(val languageCode: String) : LocalflowException(
        "Language '$languageCode' is not registered or available in this project."
    )
    
    class ParseException(message: String, cause: Throwable? = null) : LocalflowException(message, cause)
}
