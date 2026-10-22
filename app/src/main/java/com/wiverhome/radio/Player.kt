package com.wiverhome.radio

import android.os.SystemClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private fun PlayerState.status(): String = when {
    error -> "Станция недоступна, нажмите ▶, чтобы повторить"
    buffering -> "Подключение…"
    !playWhenReady -> "Пауза"
    else -> track ?: "В эфире"
}

/** Play / pause / spinner / retry icon for the main player button. */
@Composable
internal fun PlayButtonContent(state: PlayerState, iconSize: Dp) {
    val icon = Modifier.size(iconSize)
    when {
        state.error -> Icon(Icons.Default.Refresh, contentDescription = "Повторить", modifier = icon)
        state.buffering -> CircularProgressIndicator(
            modifier = Modifier.size(iconSize * 0.75f),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        state.playWhenReady -> Icon(Icons.Default.Pause, contentDescription = "Пауза", modifier = icon)
        else -> Icon(Icons.Default.PlayArrow, contentDescription = "Играть", modifier = icon)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MiniPlayer(state: PlayerState, onOpen: () -> Unit, onToggle: () -> Unit) {
    val station = state.station ?: return
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .testTag(TAG_MINI_PLAYER)
                .clickable(onClick = onOpen)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationLogo(station.favicon, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    station.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.status(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onToggle, modifier = Modifier.size(52.dp).testTag(TAG_MINI_TOGGLE)) {
                PlayButtonContent(state, 28.dp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullPlayer(
    state: PlayerState,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSleepTimer: (minutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val station = state.station ?: return
    // Телефон лёжа: по высоте не помещаются большой логотип и кнопки, поэтому логотип ставим сбоку.
    val landscape = LocalConfiguration.current.screenHeightDp < 560
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        val controls = @Composable {
            PlayerControls(state, station, isFavorite, onFavorite, onToggle, onPrevious, onNext, onSleepTimer)
        }
        if (landscape) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StationLogo(station.favicon, 160.dp, corner = 24.dp)
                Spacer(Modifier.width(24.dp))
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { controls() }
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StationLogo(station.favicon, 240.dp, corner = 28.dp)
                Spacer(Modifier.height(28.dp))
                controls()
            }
        }
    }
}

/** Название, статус и кнопки полного плеера (без логотипа). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerControls(
    state: PlayerState,
    station: Station,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSleepTimer: (minutes: Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            station.name,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            state.status(),
            style = MaterialTheme.typography.bodyLarge,
            color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier.basicMarquee(),
        )
        station.details(hideCountryCode = null).takeIf { it.isNotEmpty() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(32.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            IconButton(onClick = onPrevious, enabled = state.hasQueue, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Предыдущая станция", modifier = Modifier.size(40.dp))
            }
            FilledIconButton(onClick = onToggle, modifier = Modifier.size(84.dp)) {
                PlayButtonContent(state, 44.dp)
            }
            IconButton(onClick = onNext, enabled = state.hasQueue, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Следующая станция", modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isFavorite) "В избранном" else "В избранное")
            }
            SleepTimerButton(state.sleepAt, onSleepTimer)
        }
        if (state.sleepAt > 0) {
            // Обновляем подпись раз в несколько секунд, пока плеер открыт.
            val now by produceState(SystemClock.elapsedRealtime(), state.sleepAt) {
                while (true) {
                    value = SystemClock.elapsedRealtime()
                    delay(5_000)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                SleepTimer.label(state.sleepAt, now),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SleepTimerButton(sleepAt: Long, onSleepTimer: (minutes: Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilledTonalButton(onClick = { open = true }) {
            Icon(
                if (sleepAt > 0) Icons.Default.Bedtime else Icons.Outlined.Bedtime,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Таймер сна")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SleepTimer.OPTIONS.forEach { minutes ->
                DropdownMenuItem(
                    text = { Text(SleepTimer.duration(minutes)) },
                    onClick = { open = false; onSleepTimer(minutes) },
                )
            }
            if (sleepAt > 0) {
                DropdownMenuItem(
                    text = { Text("Выключить таймер") },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                    onClick = { open = false; onSleepTimer(0) },
                )
            }
        }
    }
}
