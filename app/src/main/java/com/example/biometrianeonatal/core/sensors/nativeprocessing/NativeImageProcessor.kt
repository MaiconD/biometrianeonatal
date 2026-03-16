package com.example.biometrianeonatal.core.sensors.nativeprocessing

import javax.inject.Inject

/**
 * Interface `NativeImageProcessor` que define um contrato reutilizado por outras camadas.
 */
interface NativeImageProcessor {
    val isNativeAvailable: Boolean

    fun processCapture(content: ByteArray): ByteArray
}

/**
 * Tipo `NoOpNativeImageProcessor` que organiza dados ou comportamento desta camada.
 */
class NoOpNativeImageProcessor @Inject constructor() : NativeImageProcessor {
    override val isNativeAvailable: Boolean = false

    override fun processCapture(content: ByteArray): ByteArray = content
}


