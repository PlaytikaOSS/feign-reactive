@file:JvmName("MonoKt")

package reactivefeign

import kotlinx.coroutines.reactor.awaitSingle
import reactivefeign.client.ReactiveHttpResponse
import reactor.core.publisher.Mono

internal suspend fun Mono<*>.awaitReactiveHttpResponse(): Any {
    val result = awaitSingle()
    if (result is ReactiveHttpResponse<*>) {
        val body = result.body()
        require(body is Mono<*>) { "Only Mono type is allowed for suspend method" }
        return body.awaitSingle()
    }

    return result
}
