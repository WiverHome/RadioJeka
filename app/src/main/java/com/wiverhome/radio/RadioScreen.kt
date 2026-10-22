package com.wiverhome.radio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
internal fun RadioScreen(vm: RadioViewModel) {
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

    // Широкий экран (планшет, телефон лёжа): регион рядом с поиском, чтобы шапка была ниже и станциям оставалось место.
    val wide = LocalConfiguration.current.screenWidthDp >= 600

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
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
                                .testTag(TAG_SEARCH),
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
                        if (wide) {
                            RegionSelector(region, vm::selectRegion, Modifier.width(320.dp).padding(start = 8.dp, end = 4.dp))
                        }
                        ThemeButton(theme, onSelect = vm::selectTheme)
                        MoreButton(
                            onAddStation = { showAddStation = true },
                            onExport = { exportFile.launch("RadioJeka-избранное.json") },
                            onImport = { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    if (!wide) {
                        RegionSelector(region, vm::selectRegion, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    }
                    GenreChips(genre, onGenre = vm::selectGenre)
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
        // На телефоне одна колонка, на планшете две-три: длинные строки на весь экран неудобно читать.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 380.dp),
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = padding,
        ) {
            fun wide(key: String? = null, content: @Composable () -> Unit) =
                item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
            val searching = query.isNotBlank()
            if (!searching && genre == null && favorites.isNotEmpty()) {
                wide { SectionHeader("Избранное") }
                gridItems(favorites, key = { "fav-" + it.id }) { StationItem(it, favorites) }
            }
            val header = when {
                searching -> "Результаты"
                genre != null -> genre?.title.orEmpty()
                else -> "Популярные"
            }
            wide { SectionHeader("$header · ${region.title}") }
            when (val state = list) {
                ListState.Loading -> wide {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                ListState.Failed -> wide {
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
                    wide {
                        Text(
                            "Ничего не найдено",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    gridItems(state.stations, key = { "list-" + it.id }) { StationItem(it, state.stations) }
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
private fun RegionSelector(region: Region, onRegion: (Region) -> Unit, modifier: Modifier) {
    SingleChoiceSegmentedButtonRow(modifier) {
        Region.entries.forEachIndexed { index, r ->
            SegmentedButton(
                selected = r == region,
                onClick = { onRegion(r) },
                shape = SegmentedButtonDefaults.itemShape(index, Region.entries.size),
            ) { Text(r.title) }
        }
    }
}

@Composable
private fun GenreChips(genre: Genre?, onGenre: (Genre?) -> Unit) {
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
        modifier = Modifier
            .testTag(TAG_STATION)
            .clickable(onClick = onClick)
            .alpha(if (isBroken && !isCurrent) 0.55f else 1f),
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
internal fun StationLogo(url: String, size: Dp, corner: Dp = 12.dp) {
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
