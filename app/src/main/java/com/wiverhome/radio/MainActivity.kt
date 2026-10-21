package com.wiverhome.radio

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: RadioViewModel = viewModel()
            val theme by vm.theme.collectAsStateWithLifecycle()
            val dark = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Status/navigation bar icons follow the chosen theme, not the system one.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            RadioTheme(dark) { RadioScreen(vm) }
        }
    }
}

@Composable
private fun RadioTheme(dark: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        // Before Material You: brand colors from the launcher icon gradient.
        dark -> darkColorScheme(
            primary = Color(0xFFFFB2BE),
            onPrimary = Color(0xFF5F1027),
            primaryContainer = Color(0xFF7E2A3E),
            secondaryContainer = Color(0xFF5C3B42),
        )
        else -> lightColorScheme(
            primary = Color(0xFFC8294F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD9DE),
            secondaryContainer = Color(0xFFFFE3E6),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun RadioScreen(vm: RadioViewModel) {
    val query by vm.query.collectAsStateWithLifecycle()
    val list by vm.list.collectAsStateWithLifecycle()
    val favorites by vm.favorites.items.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()
    val region by vm.region.collectAsStateWithLifecycle()
    val genre by vm.genre.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val brokenIds by vm.brokenIds.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val favoriteIds = favorites.mapTo(HashSet()) { it.id }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var showAddStation by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportFavorites)
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importFavorites)
    }
    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            // С кнопкой «Вернуть» — подольше, чтобы успеть нажать.
            val duration = if (message.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            val result = snackbar.showSnackbar(message.text, actionLabel = message.actionLabel, duration = duration)
            if (result == SnackbarResult.ActionPerformed) message.action?.invoke()
        }
    }

    /** [queue] is the list the station was picked from: next/previous walk through it. */
    @Composable
    fun StationItem(station: Station, queue: List<Station>) = StationRow(
        station = station,
        details = station.details(hideCountryCode = region.countryCode),
        isFavorite = station.id in favoriteIds,
        isCurrent = station.id == player.station?.id,
        isPlaying = station.id == player.station?.id && player.playWhenReady && !player.error,
        isBroken = station.id in brokenIds,
        onClick = {
            focus.clearFocus()
            vm.play(station, queue)
        },
        onFavorite = { vm.toggleFavorite(station) },
    )

    Scaffold(
        topBar = {
            // Opaque, otherwise the list scrolls visibly underneath the search and filters.
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.statusBarsPadding().padding(bottom = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { vm.query.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            placeholder = { Text("Поиск радиостанций") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { vm.query.value = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                        )
                        ThemeButton(theme, onSelect = vm::selectTheme)
                        MoreButton(
                            onAddStation = { showAddStation = true },
                            onExport = { exportFile.launch("RadioJeka-избранное.json") },
                            onImport = { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Filters(region, genre, onRegion = vm::selectRegion, onGenre = vm::selectGenre)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (player.station != null) {
                MiniPlayer(player, onOpen = { showPlayer = true }, onToggle = vm::toggle)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = padding,
        ) {
            val searching = query.isNotBlank()
            if (!searching && genre == null && favorites.isNotEmpty()) {
                item { SectionHeader("Избранное") }
                items(favorites, key = { "fav-" + it.id }) { StationItem(it, favorites) }
            }
            val header = when {
                searching -> "Результаты"
                genre != null -> genre?.title.orEmpty()
                else -> "Популярные"
            }
            item { SectionHeader("$header · ${region.title}") }
            when (val state = list) {
                ListState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                ListState.Failed -> item {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Не удалось загрузить станции. Проверьте подключение к интернету.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = vm::retry) { Text("Повторить") }
                    }
                }
                is ListState.Ready -> if (state.stations.isEmpty()) {
                    item {
                        Text(
                            "Ничего не найдено",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(state.stations, key = { "list-" + it.id }) { StationItem(it, state.stations) }
                }
            }
        }
    }

    val current = player.station
    if (showPlayer && current != null) {
        FullPlayer(
            state = player,
            isFavorite = current.id in favoriteIds,
            onFavorite = { vm.toggleFavorite(current) },
            onToggle = vm::toggle,
            onPrevious = vm::previous,
            onNext = vm::next,
            onSleepTimer = vm::setSleepTimer,
            onDismiss = { showPlayer = false },
        )
    }
    if (showAddStation) {
        AddStationDialog(onAdd = vm::addCustomStation, onDismiss = { showAddStation = false })
    }
}

@Composable
private fun MoreButton(onAddStation: () -> Unit, onExport: () -> Unit, onImport: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Ещё")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Добавить станцию по ссылке") },
                leadingIcon = { Icon(Icons.Default.AddLink, contentDescription = null) },
                onClick = { open = false; onAddStation() },
            )
            DropdownMenuItem(
                text = { Text("Сохранить избранное в файл") },
                leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) },
                onClick = { open = false; onExport() },
            )
            DropdownMenuItem(
                text = { Text("Загрузить избранное из файла") },
                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                onClick = { open = false; onImport() },
            )
        }
    }
}

/** [onAdd] возвращает текст ошибки или null, если станция добавлена. */
@Composable
private fun AddStationDialog(onAdd: (name: String, url: String) -> String?, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun submit() {
        error = onAdd(name, url)
        if (error == null) onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Своя станция") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Вставьте ссылку на поток радиостанции. Станция появится в избранном.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    label = { Text("Ссылка на поток") },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название (необязательно)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
            }
        },
        confirmButton = { TextButton(onClick = ::submit) { Text("Добавить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun ThemeButton(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    fun icon(mode: ThemeMode) = when (mode) {
        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
        ThemeMode.LIGHT -> Icons.Default.LightMode
        ThemeMode.DARK -> Icons.Default.DarkMode
    }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(icon(current), contentDescription = "Тема: ${current.title}")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.title) },
                    leadingIcon = { Icon(icon(mode), contentDescription = null) },
                    trailingIcon = if (mode == current) {
                        { Icon(Icons.Default.Check, contentDescription = "Выбрана") }
                    } else {
                        null
                    },
                    onClick = {
                        open = false
                        onSelect(mode)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Filters(
    region: Region,
    genre: Genre?,
    onRegion: (Region) -> Unit,
    onGenre: (Genre?) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Region.entries.forEachIndexed { index, r ->
            SegmentedButton(
                selected = r == region,
                onClick = { onRegion(r) },
                shape = SegmentedButtonDefaults.itemShape(index, Region.entries.size),
            ) { Text(r.title) }
        }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(selected = genre == null, onClick = { onGenre(null) }, label = { Text("Все") })
        }
        items(GENRES, key = { it.title }) { g ->
            FilterChip(selected = g == genre, onClick = { onGenre(g) }, label = { Text(g.title) })
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun StationRow(
    station: Station,
    details: String,
    isFavorite: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isBroken: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
) {
    ListItem(
        // Неотвечающая станция приглушена, но остаётся доступной: поток могли уже починить.
        modifier = Modifier.clickable(onClick = onClick).alpha(if (isBroken && !isCurrent) 0.55f else 1f),
        colors = if (isCurrent) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.colors()
        },
        leadingContent = {
            Box {
                StationLogo(station.favicon, 48.dp)
                if (isPlaying) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Играет", tint = Color.White)
                    }
                }
            }
        },
        headlineContent = {
            Text(station.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = when {
            isBroken -> {
                {
                    Text(
                        listOf("Не отвечает", details).filter { it.isNotEmpty() }.joinToString(" · "),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            details.isNotEmpty() -> {
                { Text(details, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            else -> null
        },
        trailingContent = {
            IconButton(onClick = onFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Убрать из избранного" else "В избранное",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun StationLogo(url: String, size: Dp, corner: Dp = 12.dp) {
    var loaded by remember(url) { mutableStateOf(false) }
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { loaded = true },
            )
        }
        if (!loaded) {
            Icon(
                Icons.Default.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.45f),
            )
        }
    }
}

private fun PlayerState.status(): String = when {
    error -> "Станция недоступна, нажмите ▶, чтобы повторить"
    buffering -> "Подключение…"
    !playWhenReady -> "Пауза"
    else -> track ?: "В эфире"
}

/** Play / pause / spinner / retry icon for the main player button. */
@Composable
private fun PlayButtonContent(state: PlayerState, iconSize: Dp) {
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
private fun MiniPlayer(state: PlayerState, onOpen: () -> Unit, onToggle: () -> Unit) {
    val station = state.station ?: return
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            Modifier
                .fillMaxWidth()
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
            FilledIconButton(onClick = onToggle, modifier = Modifier.size(52.dp)) {
                PlayButtonContent(state, 28.dp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FullPlayer(
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
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
