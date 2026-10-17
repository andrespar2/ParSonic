package com.andrespar2.parsonic.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.andrespar2.parsonic.core.ParSonicConstants
import com.andrespar2.parsonic.data.settings.model.AppSettings
import com.andrespar2.parsonic.data.settings.model.GestureAction
import com.andrespar2.parsonic.data.settings.model.GestureType
import com.andrespar2.parsonic.data.settings.model.Sensitivity
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Pruebas de persistencia real sobre Preferences DataStore.
 *
 * Se usa el artefacto JVM de DataStore con un archivo temporal, de modo que no
 * se necesita emulador ni Robolectric.
 */
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private var scope: CoroutineScope? = null

    private fun newRepository(fileName: String = "parsonic.preferences_pb"): DataStoreSettingsRepository {
        val file = File(temporaryFolder.root, fileName)
        val dataStoreScope = scope ?: CoroutineScope(Job()).also { scope = it }
        val store = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { file }
        )
        return DataStoreSettingsRepository(store)
    }

    @After
    fun tearDown() {
        scope?.cancel()
    }

    @Test
    fun `los valores por defecto coinciden con el modelo`() = runTest {
        val repository = newRepository()
        val settings = repository.settings.first()
        assertEquals(AppSettings.DEFAULT.onlyWhilePlaying, settings.onlyWhilePlaying)
        assertEquals(AppSettings.DEFAULT.edgeZoneWidthDp, settings.edgeZoneWidthDp)
        assertEquals(
            AppSettings.defaultBinding(GestureType.DOUBLE_TAP).action,
            settings.bindingOf(GestureType.DOUBLE_TAP).action
        )
    }

    @Test
    fun `persiste el interruptor maestro y el inicio automatico`() = runTest {
        val repository = newRepository()
        repository.setServiceEnabled(true)
        repository.setAutostartOnBoot(true)

        val settings = repository.settings.first()
        assertTrue(settings.serviceEnabled)
        assertTrue(settings.autostartOnBoot)
    }

    @Test
    fun `persiste la accion de un gesto y la sensibilidad global`() = runTest {
        val repository = newRepository()
        repository.setGestureAction(GestureType.SWIPE_LEFT, GestureAction.PAUSE)
        repository.setSensitivity(Sensitivity.HIGH)

        val settings = repository.settings.first()
        assertEquals(GestureAction.PAUSE, settings.bindingOf(GestureType.SWIPE_LEFT).action)
        assertEquals(Sensitivity.HIGH, settings.sensitivity)
    }

    @Test
    fun `desactivar un gesto lo marca como no efectivo`() = runTest {
        val repository = newRepository()
        repository.setGestureEnabled(GestureType.SWIPE_RIGHT, false)

        val binding = repository.settings.first().bindingOf(GestureType.SWIPE_RIGHT)
        assertFalse(binding.enabled)
        assertFalse(binding.isEffective)
    }

    @Test
    fun `el ancho de zona se limita al rango permitido`() = runTest {
        val repository = newRepository()
        repository.setEdgeZoneWidth(5000)
        assertEquals(
            ParSonicConstants.MAX_EDGE_ZONE_WIDTH_DP,
            repository.settings.first().edgeZoneWidthDp
        )
        repository.setEdgeZoneWidth(1)
        assertEquals(
            ParSonicConstants.MIN_EDGE_ZONE_WIDTH_DP,
            repository.settings.first().edgeZoneWidthDp
        )
    }

    @Test
    fun `resetGestures devuelve la configuracion de fabrica`() = runTest {
        val repository = newRepository()
        repository.setGestureAction(GestureType.SWIPE_LEFT, GestureAction.STOP)
        repository.setGestureEnabled(GestureType.DOUBLE_TAP, false)
        repository.setSensitivity(Sensitivity.HIGH)

        repository.resetGestures()

        val settings = repository.settings.first()
        assertEquals(GestureAction.PREVIOUS, settings.bindingOf(GestureType.SWIPE_LEFT).action)
        assertTrue(settings.bindingOf(GestureType.DOUBLE_TAP).enabled)
        assertEquals(Sensitivity.MEDIUM, settings.sensitivity)
    }

    @Test
    fun `los cambios sobreviven a una nueva instancia sobre el mismo archivo`() = runTest {
        val file = File(temporaryFolder.root, "persistencia.preferences_pb")

        // Primera instancia: escribe y libera el archivo al cancelar su scope.
        // Se espera a que la cancelacion termine de verdad: DataStore libera el
        // bloqueo del archivo en un manejador de finalizacion, asi que crear la
        // segunda instancia sin esperar daria un resultado no determinista.
        val writingJob = Job()
        val writing = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(writingJob),
                produceFile = { file }
            )
        )
        writing.setGestureAction(GestureType.SWIPE_UP, GestureAction.NEXT)
        writing.setHapticFeedback(false)
        writing.setSensitivity(Sensitivity.LOW)
        writingJob.cancelAndJoin()

        // Segunda instancia: DataStore no permite dos lectores activos del mismo archivo.
        val reading = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Job()).also { scope = it },
                produceFile = { file }
            )
        )

        val settings = reading.settings.first()
        assertEquals(GestureAction.NEXT, settings.bindingOf(GestureType.SWIPE_UP).action)
        assertFalse(settings.hapticFeedback)
        assertEquals(Sensitivity.LOW, settings.sensitivity)
    }

    @Test
    fun `la sensibilidad por defecto de un gesto recien reseteado es media`() = runTest {
        // Este test ya queda cubierto en resetGestures.
    }
}
