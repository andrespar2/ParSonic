package com.andrespar2.parsonic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.andrespar2.parsonic.ui.ParSonicApp
import com.andrespar2.parsonic.ui.theme.ParSonicTheme

/**
 * Unica actividad de ParSonic.
 *
 * La interfaz es enteramente Compose y vive en [ParSonicApp]; aqui solo se monta
 * el tema y el contenedor de dependencias de la aplicacion.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as ParSonicApplication).container
        setContent {
            ParSonicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ParSonicApp(container = container)
                }
            }
        }
    }
}
