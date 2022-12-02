package dev.virefire.viira

import org.junit.jupiter.api.Test
import java.net.URI

internal class RequestProcessorTest {
    @Test
    fun splitQuery() {
        run {
            val result = splitQuery(URI("https://example.com/test?param=value"))
            assert(result["param"] == "value")
        }
        run {
            val result = splitQuery(URI("https://example.com/test?param="))
            assert(result["param"] == "")
        }
        run {
            val result = splitQuery(URI("https://example.com/test?param"))
            assert(!result.containsKey("param"))
        }
        run {
            val result = splitQuery(URI("https://example.com/test?param=value1&param=value2"))
            assert(result["param"] == "value2")
        }
    }
}