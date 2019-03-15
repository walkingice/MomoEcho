package net.julianchu.momoecho

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    lateinit var mainScope: CoroutineScope
    lateinit var playbackScope: CoroutineScope

    @Before
    fun createScope() {
        mainScope = CoroutineScope(Dispatchers.Unconfined)
        playbackScope = CoroutineScope(newSingleThreadContext("playback"))
    }

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun test_coroutine() {
        println("Lets go \t\t ${Thread.currentThread().name}")
        val job = goCoroutine(playbackScope)
        println("Coroutine fired \t\t ${Thread.currentThread().name}")

        // for waiting
        runBlocking {
            job.join()
        }
    }

    private fun goCoroutine(scope: CoroutineScope): Job {
        val job = scope.launch {
            for (i in 1..5) {
                delay(1000)
                println("world $i \t\t ${Thread.currentThread().name}")
            }
        }

        println("Hello \t\t ${Thread.currentThread().name}")

        // simulate cancellation
        timer(scope, 3000) {
            println("callback invoked \t\t ${Thread.currentThread().name}")
            job.cancel()
        }

        return job
    }

    private fun timer(
        scope: CoroutineScope,
        period: Long,
        callback: () -> Unit
    ): Job {
        return scope.launch {
            delay(period)
            // TODO: should launch in main thread
            mainScope.launch {
                callback()
            }
        }
    }
}
