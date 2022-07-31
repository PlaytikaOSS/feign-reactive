package reactivefeign

import com.fasterxml.jackson.core.JsonProcessingException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import reactivefeign.resttemplate.client.RestTemplateFakeReactiveFeign
import reactivefeign.testcase.SuspendIceCreamServiceApi
import reactivefeign.testcase.domain.OrderGenerator
import reactor.core.publisher.Mono
import reactor.netty.DisposableServer
import reactor.netty.http.HttpProtocol
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import reactor.netty.http.server.HttpServerRoutes
import java.time.Duration

class SuspendTest {
    companion object {
        private lateinit var server: DisposableServer
        private const val DELAY_IN_MILLIS: Long = 500L
        private val cannedValue = OrderGenerator().generate(1)

        @BeforeClass
        @JvmStatic
        @Throws(JsonProcessingException::class)
        fun startServer() {
            val data = TestUtils.MAPPER.writeValueAsString(cannedValue).toByteArray()
            server = HttpServer.create()
                .protocol(HttpProtocol.HTTP11, HttpProtocol.H2C)
                .route { routes: HttpServerRoutes ->
                    routes[
                        "/icecream/orders/1", { req: HttpServerRequest?, res: HttpServerResponse ->
                            res.header("Content-Type", "application/json")
                            Mono.delay(Duration.ofMillis(DELAY_IN_MILLIS))
                                .thenEmpty(res.sendByteArray(Mono.just(data)))
                        }
                    ]
                }
                .bindNow()
        }

        @JvmStatic
        @AfterClass
        fun stopServer() {
            server.disposeNow()
        }
    }

    @Test
    fun shouldRun(): Unit = runBlocking {
        val client = RestTemplateFakeReactiveFeign
            .builder<SuspendIceCreamServiceApi>()
            .target(
                SuspendIceCreamServiceApi::class.java,
                "http://localhost:" + server.port()
            )

        val firstOrder = client.findOrder(orderId = 1)

        assertThat(firstOrder).usingRecursiveComparison().isEqualTo(cannedValue)
    }
}
