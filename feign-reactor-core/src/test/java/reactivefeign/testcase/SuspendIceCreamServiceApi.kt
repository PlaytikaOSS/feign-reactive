package reactivefeign.testcase

import feign.Param
import feign.RequestLine
import reactivefeign.testcase.domain.IceCreamOrder

interface SuspendIceCreamServiceApi {
    @RequestLine("GET /icecream/orders/{orderId}")
    suspend fun findOrder(@Param("orderId") orderId: Int): IceCreamOrder
}
