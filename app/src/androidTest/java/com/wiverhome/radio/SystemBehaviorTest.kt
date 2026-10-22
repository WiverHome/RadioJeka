package com.wiverhome.radio

import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Поведение, которое зависит от версии Android (в CI — Android 14 и Android 16):
 * экран «от края до края» без наложения на системные панели, системная кнопка «Назад»
 * и воспроизведение в фоне с уведомлением (без отдельного разрешения на уведомления).
 */
@RunWith(AndroidJUnit4::class)
class SystemBehaviorTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun start() {
        listOf("favorites", "settings", "broken").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitFor(hasTestTag(TAG_STATION))
    }

    @After
    fun stop() {
        scenario.close()
    }

    @Test
    fun contentIsEdgeToEdgeButClearOfSystemBars() {
        compose.onAllNodesWithTag(TAG_STATION).onFirst().performClick()
        waitFor(hasTestTag(TAG_MINI_TOGGLE))

        var top = 0
        var bottom = 0
        var height = 0
        scenario.onActivity {
            val bars = ViewCompat.getRootWindowInsets(it.window.decorView)!!
                .getInsets(WindowInsetsCompat.Type.systemBars())
            top = bars.top
            bottom = bars.bottom
            height = it.window.decorView.height
        }
        // Приложение рисует под системными панелями (edge-to-edge)…
        assertEquals(height.toFloat(), compose.onRoot().fetchSemanticsNode().boundsInWindow.bottom, 1f)
        // …но поиск и кнопки не прячутся под строкой состояния и панелью навигации.
        val search = compose.onNodeWithTag(TAG_SEARCH).fetchSemanticsNode().boundsInWindow
        assertTrue("поиск залез под строку состояния: ${search.top} < $top", search.top >= top)
        val toggle = compose.onNodeWithTag(TAG_MINI_TOGGLE).fetchSemanticsNode().boundsInWindow
        assertTrue("кнопка плеера под панелью навигации: ${toggle.bottom} > ${height - bottom}", toggle.bottom <= height - bottom)
    }

    @Test
    fun systemBackClosesFullPlayerButKeepsApp() {
        compose.onAllNodesWithTag(TAG_STATION).onFirst().performClick()
        waitFor(hasTestTag(TAG_MINI_PLAYER))
        compose.onNodeWithTag(TAG_MINI_PLAYER).performClick()
        waitFor(hasText("Таймер сна"))

        Espresso.pressBack()

        waitGone(hasText("Таймер сна"))
        compose.onNodeWithTag(TAG_SEARCH).assertExists()
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
    }

    @Test
    fun playbackContinuesInBackgroundWithNotification() {
        // Своя станция с надёжным потоком, чтобы тест не зависел от доступности конкретного радио.
        compose.onNodeWithContentDescription("Ещё").performClick()
        compose.onNodeWithText("Добавить станцию по ссылке").performClick()
        compose.onNode(hasSetTextAction() and hasText("Ссылка на поток", substring = true))
            .performTextInput("https://stream.radioparadise.com/mp3-128")
        compose.onNode(hasSetTextAction() and hasText("Название", substring = true)).performTextInput("Фоновый тест")
        compose.onNodeWithText("Добавить").performClick()
        waitFor(hasTestTag(TAG_STATION) and hasText("Фоновый тест"))
        compose.onNode(hasTestTag(TAG_STATION) and hasText("Фоновый тест")).performClick()
        waitFor(hasTestTag(TAG_MINI_PLAYER))

        // Приложение уходит в фон: экран остановлен, служба воспроизведения должна продолжать работу.
        scenario.moveToState(Lifecycle.State.CREATED)
        val notifications = context.getSystemService(NotificationManager::class.java)
        val deadline = SystemClock.elapsedRealtime() + 30_000
        while (notifications.activeNotifications.isEmpty() && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(500)
        }
        assertTrue("нет уведомления плеера в фоне", notifications.activeNotifications.isNotEmpty())
        SystemClock.sleep(5_000)

        // Вернулись — радио не встало на паузу само.
        scenario.moveToState(Lifecycle.State.RESUMED)
        waitFor(hasTestTag(TAG_MINI_PLAYER))
        val paused = hasText("Пауза") and hasAnyAncestor(hasTestTag(TAG_MINI_PLAYER))
        assertTrue("плеер встал на паузу в фоне", compose.onAllNodes(paused, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        compose.onNode(hasTestTag(TAG_MINI_TOGGLE) and hasContentDescription("Играть")).assertDoesNotExist()

        // Выключаем радио, чтобы оно не играло в следующих тестах.
        compose.onNodeWithTag(TAG_MINI_TOGGLE).performClick()
    }

    private fun waitFor(matcher: SemanticsMatcher, timeoutMs: Long = 15_000) {
        compose.waitUntil(timeoutMs) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitGone(matcher: SemanticsMatcher, timeoutMs: Long = 15_000) {
        compose.waitUntil(timeoutMs) { compose.onAllNodes(matcher).fetchSemanticsNodes().isEmpty() }
    }
}
