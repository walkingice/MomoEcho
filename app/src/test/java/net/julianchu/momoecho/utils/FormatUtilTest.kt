package net.julianchu.momoecho.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.julianchu.momoecho.BuildConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, constants = BuildConfig::class)
class FormatUtilsTest {

    @Before
    @Throws(Exception::class)
    fun setUp() {
    }

    @After
    @Throws(Exception::class)
    fun cleanUp() {
    }

    @Test
    @Throws(Exception::class)
    fun testCoroutine() = runBlocking {
        var count = 0
        val myScope = GlobalScope
        count++
        val z = myScope.launch {
            delay(100)
            count = 10
        }

        count = 1
        assertEquals(1, count)
        z.join()
        assertEquals(10, count)
    }
}
