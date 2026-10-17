package com.andrespar2.parsonic.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrespar2.parsonic.data.media.NowPlaying
import com.andrespar2.parsonic.data.media.PlaybackStatusMapper
import com.andrespar2.parsonic.di.AppContainer
import com.andrespar2.parsonic.ui.ParSonicViewModelFactory
import com.andrespar2.parsonic.ui.components.ParSonicCard
import com.andrespar2.parsonic.ui.components.SectionTitle
import com.andrespar2.parsonic.ui.theme.ParRed
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(container: AppContainer) {
    val viewModel: PlayerViewModel = viewModel(
        factory = remember(container) { ParSonicViewModelFactory(container) }
    )
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        if (nowPlaying == null) {
            EmptyState()
            Spacer(Modifier.height(24.dp))
            return@Column
        }

        val track = nowPlaying!!
        val position = rememberLivePosition(track)

        Artwork(track)

        Spacer(Modifier.height(18.dp))
        Text(
            text = track.title.ifBlank { "Sin titulo" },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = track.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${track.appLabel} · ${track.status.label}",
            style = MaterialTheme.typography.labelSmall,
            color = ParRed
        )

        Spacer(Modifier.height(20.dp))
        ProgressBar(position = position, duration = track.durationMs)

        Spacer(Modifier.height(24.dp))
        TransportControls(
            nowPlaying = track,
            onPrevious = viewModel::previous,
            onToggle = viewModel::togglePlayPause,
            onNext = viewModel::next
        )

        Spacer(Modifier.height(20.dp))
        SeekControls(
            enabled = track.canSeek,
            onBackward = viewModel::seekBackward,
            onForward = viewModel::seekForward
        )

        Spacer(Modifier.height(20.dp))
        VolumeControls(
            onLower = viewModel::volumeDown,
            onRaise = viewModel::volumeUp
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyState() {
    ParSonicCard {
        Text(
            text = "Sin reproduccion activa",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "ParSonic controla el reproductor que este sonando en este momento, sea cual sea: " +
                "Spotify, YouTube Music, VLC, un reproductor local o cualquier app que publique una " +
                "sesion multimedia.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Artwork(track: NowPlaying) {
    val shape = RoundedCornerShape(24.dp)
    val bitmap = track.artwork
    Box(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = track.appLabel.take(1).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                color = ParRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProgressBar(position: Long, duration: Long) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val fraction = if (duration > 0L) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = ParRed,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = PlaybackStatusMapper.formatDuration(position),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = PlaybackStatusMapper.formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransportControls(
    nowPlaying: NowPlaying,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, enabled = nowPlaying.canSkipPrevious, modifier = Modifier.size(52.dp)) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Pista anterior",
                tint = if (nowPlaying.canSkipPrevious) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.width(20.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ParRed),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onToggle, enabled = nowPlaying.canTogglePlayback) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = if (nowPlaying.isPlaying) "Pausar" else "Reproducir",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
        Spacer(Modifier.width(20.dp))
        IconButton(onClick = onNext, enabled = nowPlaying.canSkipNext, modifier = Modifier.size(52.dp)) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Pista siguiente",
                tint = if (nowPlaying.canSkipNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun SeekControls(enabled: Boolean, onBackward: () -> Unit, onForward: () -> Unit) {
    SectionTitle("Dentro de la pista")
    ParSonicCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = onBackward, enabled = enabled) {
                Text("-10 s", color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onForward, enabled = enabled) {
                Text("+10 s", color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!enabled) {
            Text(
                text = "Este reproductor no expone el control de posicion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VolumeControls(onLower: () -> Unit, onRaise: () -> Unit) {
    SectionTitle("Volumen multimedia")
    ParSonicCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = onLower) { Text("-", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = onRaise) { Text("+", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface) }
        }
        Text(
            text = "Se ajusta el volumen multimedia del sistema, que es el que respetan todos los " +
                "reproductores.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Posicion de reproduccion que avanza sola mientras suena la pista.
 *
 * La sesion solo informa de la posicion en el instante del ultimo cambio, asi
 * que la interfaz la extrapola segundo a segundo. Es un temporizador de interfaz
 * que solo corre mientras esta pantalla esta visible y hay musica sonando; no
 * afecta al consumo en segundo plano.
 */
@Composable
private fun rememberLivePosition(track: NowPlaying): Long {
    var position by remember { mutableLongStateOf(track.positionMs) }
    LaunchedEffect(track.title, track.artist, track.isPlaying, track.positionMs) {
        position = track.positionMs
        if (!track.isPlaying) return@LaunchedEffect
        while (true) {
            delay(1_000L)
            val next = position + 1_000L
            position = if (track.durationMs > 0L) next.coerceAtMost(track.durationMs) else next
        }
    }
    return position
}
