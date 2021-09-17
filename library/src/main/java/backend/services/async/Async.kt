package backend.services.async

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

private const val NUM_THREADS = 2

object Async : CoroutineScope {
    override val coroutineContext =
        Executors.newFixedThreadPool(NUM_THREADS, ThreadFactory {
            Thread(it, "BackendServicesAndroidClient-Thread")
        }).asCoroutineDispatcher()
}