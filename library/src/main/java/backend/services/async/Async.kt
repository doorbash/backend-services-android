package backend.services.async

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executors

//private const val NUM_THREADS = 2

object Async : CoroutineScope {
    //    override val coroutineContext =
//        Executors.newFixedThreadPool(NUM_THREADS, ThreadFactory {
//            Thread(it, "BackendServicesAndroidClient-Thread")
//        }).asCoroutineDispatcher()
    override val coroutineContext = Executors.newSingleThreadExecutor {
        Thread(it, "BackendServicesAndroidClient-Thread")
    }.asCoroutineDispatcher()
//    override val coroutineContext = Dispatchers.IO.limitedParallelism(1)

    private val mutex = Mutex()

    fun launchWithLockAndTimeout(timeout: Long, block: suspend CoroutineScope.() -> Unit): Job {
        return launch {
            withLockAndTimeout(timeout, block)
        }
    }

    suspend fun <T> withLockAndTimeout(timeout: Long, block: suspend CoroutineScope.() -> T): T {
        return withContext(coroutineContext) {
            mutex.withLock {
                withTimeout(timeout) {
                    block.invoke(this)
                }
            }
        }
    }
}