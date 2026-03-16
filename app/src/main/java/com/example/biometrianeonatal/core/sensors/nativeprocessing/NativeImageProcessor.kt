package com.example.biometrianeonatal.core.sensors.nativeprocessing

import javax.inject.Inject

interface NativeImageProcessor {
    val isNativeAvailable: Boolean

    fun processCapture(content: ByteArray): ByteArray
}

class NoOpNativeImageProcessor @Inject constructor() : NativeImageProcessor {
    override val isNativeAvailable: Boolean = false

    override fun processCapture(content: ByteArray): ByteArray = content
}


