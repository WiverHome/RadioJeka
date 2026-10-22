package com.wiverhome.radio

import android.content.Context
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Тесты интерфейса на эмуляторе или телефоне: главный список, поиск, избранное, плеер, таймер сна, своя станция.
 * Список берётся из встроенного каталога, поэтому сеть не нужна (звук в тестах не проверяется).
 */
@RunWith(AndroidJUnit4::class)
class RadioScreenTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun start() {
        // Каждый тест начинается с чистого избранного и настроек по умолчанию (регион «Россия»).
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        listOf("favorites", "settings", "broken").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitFor(hasText("Популярные · Россия"))
        waitFor(hasTestTag(TAG_STATION))
    }

    @After
    fun stop() {
        scenario.close()
    }

    @Test
    fun mainListShowsPopularStations() {
        compose.onNodeWithText("Популярные · Россия").assertExists()
        assert(compose.onAllNodesWithTag(TAG_STATION).fetchSemanticsNodes().size > 3)
        compose.onNodeWithText("Россия").assertExists()
        compose.onNodeWithText("Весь мир").assertExists()
    }

    @Test
    fun searchFindsStationByName() {
        compose.onNodeWithTag(TAG_SEARCH).performTextInput("Europa")
        waitFor(hasText("Результаты · Россия"))
        waitFor(hasTestTag(TAG_STATION) and hasText("Europa", substring = true, ignoreCase = true))
    }

    @Test
    fun searchShowsNothingFound() {
        compose.onNodeWithTag(TAG_SEARCH).performTextInput("zzqxwnosuchstation")
        waitFor(hasText("Ничего не найдено"))
    }

    @Test
    fun favoriteCanBeAddedAndRemoved() {
        compose.onAllNodesWithContentDescription("В избранное").onFirst().performClick()
        waitFor(hasText("Избранное"))
        compose.onAllNodesWithContentDescription("Убрать из избранного").onFirst().performClick()
        waitGone(hasText("Избранное"))
    }

    @Test
    fun stationOpensMiniAndFullPlayer() {
        compose.onAllNodesWithTag(TAG_STATION).onFirst().performClick()
        waitFor(hasTestTag(TAG_MINI_PLAYER))
        compose.onNodeWithTag(TAG_MINI_PLAYER).performClick()
        waitFor(hasText("Таймер сна"))
        compose.onNodeWithContentDescription("Следующая станция").assertExists()
        compose.onNodeWithContentDescription("Предыдущая станция").assertExists()
    }

    @Test
    fun sleepTimerCanBeSetAndCancelled() {
        compose.onAllNodesWithTag(TAG_STATION).onFirst().performClick()
        waitFor(hasTestTag(TAG_MINI_PLAYER))
        compose.onNodeWithTag(TAG_MINI_PLAYER).performClick()
        waitFor(hasText("Таймер сна"))

        compose.onNodeWithText("Таймер сна").performClick()
        compose.onNodeWithText("15 мин").performClick()
        waitFor(hasText("Выключится через 15 мин"))

        compose.onNodeWithText("Таймер сна").performClick()
        compose.onNodeWithText("Выключить таймер").performClick()
        waitGone(hasText("Выключится через", substring = true))
    }

    @Test
    fun customStationIsCheckedAndAddedToFavorites() {
        compose.onNodeWithContentDescription("Ещё").performClick()
        compose.onNodeWithText("Добавить станцию по ссылке").performClick()
        waitFor(hasText("Своя станция"))

        val link = hasSetTextAction() and hasText("Ссылка на поток", substring = true)
        compose.onNode(link).performTextInput("ftp://example.com/live")
        compose.onNodeWithText("Добавить").performClick()
        waitFor(hasText("Ссылка должна начинаться с http:// или https://"))

        compose.onNode(link).performTextClearance()
        compose.onNode(link).performTextInput("https://example.com/live.mp3")
        compose.onNode(hasSetTextAction() and hasText("Название", substring = true)).performTextInput("Тестовая волна")
        compose.onNodeWithText("Добавить").performClick()

        waitGone(hasText("Своя станция"))
        waitFor(hasText("Избранное"))
        waitFor(hasTestTag(TAG_STATION) and hasText("Тестовая волна"))
    }

    private fun waitFor(matcher: SemanticsMatcher, timeoutMs: Long = 15_000) {
        compose.waitUntil(timeoutMs) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitGone(matcher: SemanticsMatcher, timeoutMs: Long = 15_000) {
        compose.waitUntil(timeoutMs) { compose.onAllNodes(matcher).fetchSemanticsNodes().isEmpty() }
    }
}
