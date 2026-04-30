package xyz.chambaz.tilde

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.chambaz.tilde.data.AppInfo

class AppFilterTest {

    private fun app(label: String) = AppInfo(packageName = label, label = label)

    @Test
    fun blank_query_returns_all() {
        val apps = listOf(app("Foo"), app("Bar"), app("Baz"))
        assertEquals(apps, filterApps("", apps))
    }

    @Test
    fun single_char_match() {
        val alpha = app("Alpha"); val beta = app("Beta"); val gamma = app("Gamma")
        val result = filterApps("b", listOf(alpha, beta, gamma))
        assertEquals(listOf(beta), result)
    }

    @Test
    fun subsequence_match_non_contiguous() {
        val apps = listOf(app("Google Maps"), app("Gmail"))
        val result = filterApps("gm", apps)
        assertEquals(2, result.size)
    }

    @Test
    fun case_insensitive() {
        val settings = app("Settings"); val calendar = app("Calendar")
        val result = filterApps("SET", listOf(settings, calendar))
        assertEquals(listOf(settings), result)
    }

    @Test
    fun no_match_returns_empty() {
        val apps = listOf(app("Alpha"), app("Beta"))
        assertTrue(filterApps("xyz", apps).isEmpty())
    }

    @Test
    fun ranked_by_earliest_match_start() {
        // "a" matches "banana" at index 1, "alpha" at index 0 — alpha comes first
        val apps = listOf(app("banana"), app("alpha"))
        val result = filterApps("a", apps)
        assertEquals("alpha", result[0].label)
        assertEquals("banana", result[1].label)
    }
}
