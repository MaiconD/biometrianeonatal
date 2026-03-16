package com.example.biometrianeonatal

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Teste instrumentado mínimo para confirmar o pacote Android gerado em tempo de execução.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // O contexto retornado pelo instrumentation deve apontar para o aplicativo sob teste.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.biometrianeonatal", appContext.packageName)
    }
}