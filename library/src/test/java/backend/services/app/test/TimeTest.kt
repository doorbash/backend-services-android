package backend.services.app.test

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.ZonedDateTime

class TimeTest {
    @Test
    fun `parse iso time`() {
        val time = "2021-12-18T23:54:29Z"
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(time).apply {
            println(this)
        }
    }
}