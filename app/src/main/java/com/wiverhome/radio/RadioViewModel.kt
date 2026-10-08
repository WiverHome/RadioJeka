package com.wiverhome.radio

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

sealed interface ListState {
    data object Loading : ListState
    data class Ready(val stations: List<Station>) : ListState
    data object Failed : ListState
}

data class PlayerState(
    val station: Station? = null,
    val playWhenReady: Boolean = false,
    val buffering: Boolean = false,
    val track: String? = null,
    val error: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class RadioViewModel(app: Application) : AndroidViewModel(app) {
    val favorites = FavoritesStore(app)
    val query = MutableStateFlow("")
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _region = MutableStateFlow(
        Region.entries.firstOrNull { it.name == prefs.getString(KEY_REGION, null) } ?: Region.RUSSIA,
    )
    val region: StateFlow<Region> = _region.asStateFlow()
    private val _genre = MutableStateFlow<Genre?>(null)
    val genre: StateFlow<Genre?> = _genre.asStateFlow()
    private val reload = MutableStateFlow(0)

    val list: StateFlow<ListState> = combine(
        query.map { it.trim() }.distinctUntilChanged().debounce { if (it.isEmpty()) 0L else 400L },
        _region,
        _genre,
        reload,
    ) { q, region, genre, _ -> Triple(q, region, genre) }
        .flatMapLatest { (q, region, genre) ->
            flow {
                emit(ListState.Loading)
                val result = try {
                    ListState.Ready(RadioApi.stations(q, region.countryCode, genre?.tags.orEmpty()))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ListState.Failed
                }
                emit(result)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ListState.Loading)

    private val _player = MutableStateFlow(PlayerState())
    val player: StateFlow<PlayerState> = _player.asStateFlow()

    private val controllerFuture = MediaController.Builder(
        app, SessionToken(app, ComponentName(app, PlaybackService::class.java)),
    ).buildAsync()
    private var controller: MediaController? = null
    private var current: Station? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
    }

    init {
        viewModelScope.launch {
            try {
                val c = controllerFuture.await()
                controller = c
                c.addListener(listener)
                sync(c)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Service unavailable: the list still works, playback just won't start.
            }
        }
    }

    fun retry() {
        reload.value++
    }

    fun selectRegion(region: Region) {
        _region.value = region
        prefs.edit().putString(KEY_REGION, region.name).apply()
    }

    /** Tapping the selected genre again clears it. */
    fun selectGenre(genre: Genre?) {
        _genre.value = if (genre == _genre.value) null else genre
    }

    fun toggleFavorite(station: Station) = favorites.toggle(station)

    fun play(station: Station) {
        val c = controller ?: return
        if (current?.id == station.id) {
            toggle()
            return
        }
        current = station
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setStation(station.name)
            .setArtworkUri(station.favicon.takeIf { it.isNotBlank() }?.let(Uri::parse))
            .setExtras(Bundle().apply { putString(EXTRA_STATION, station.toJson().toString()) })
            .build()
        val item = MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(station.url)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(station.url)).build())
            .setMediaMetadata(metadata)
            .build()
        _player.value = PlayerState(station = station, playWhenReady = true, buffering = true)
        c.setMediaItem(item)
        c.prepare()
        c.play()
    }

    fun toggle() {
        val c = controller ?: return
        when {
            c.playerError != null || c.playbackState == Player.STATE_IDLE -> {
                c.prepare()
                c.play()
            }
            c.playWhenReady -> c.pause()
            else -> {
                // Live radio: resume from "now", not from where it was paused.
                c.seekToDefaultPosition()
                c.play()
            }
        }
    }

    private fun sync(p: Player) {
        val item = p.currentMediaItem
        val station = item?.let {
            current?.takeIf { s -> s.id == it.mediaId }
                ?: it.mediaMetadata.extras?.getString(EXTRA_STATION)?.let { json -> Station.fromJson(JSONObject(json)) }
        }
        current = station
        _player.value = PlayerState(
            station = station,
            playWhenReady = p.playWhenReady,
            buffering = p.playWhenReady && p.playbackState == Player.STATE_BUFFERING,
            track = p.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() && it != station?.name },
            error = p.playerError != null,
        )
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
    }

    private companion object {
        const val EXTRA_STATION = "station"
        const val KEY_REGION = "region"
    }
}
