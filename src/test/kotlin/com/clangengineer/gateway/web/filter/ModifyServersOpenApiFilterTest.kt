package com.clangengineer.gateway.web.filter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream

class ModifyServersOpenApiFilterTest {

    private val filterChain: GatewayFilterChain = mock(GatewayFilterChain::class.java)
    private val captor: ArgumentCaptor<ServerWebExchange> = ArgumentCaptor.forClass(ServerWebExchange::class.java)

    @BeforeEach
    fun setup() {
        `when`(filterChain.filter(captor.capture())).thenReturn(Mono.empty())
    }

    @Test
    fun shouldCallCreateModifyServersOpenApiInterceptorWhenGetOpenApiSpec() {
        // dummy api url to filter
        val sample_url = "/services/service-test/instance-test/v3/api-docs"

        // create request
        val request = MockServerHttpRequest.get(sample_url).build()
        val exchange = MockServerWebExchange.from(request)

        // apply the filter to the request
        val modifyServersOpenApiFilter = spy(ModifyServersOpenApiFilter())
        modifyServersOpenApiFilter.filter(exchange, filterChain).subscribe()

        verify(modifyServersOpenApiFilter, times(1))
            .createModifyServersOpenApiInterceptor(sample_url, exchange.response, exchange.response.bufferFactory())
    }

    @Test
    fun shouldNotCallCreateModifyServersOpenApiInterceptorWhenNotGetOpenApiSpec() {
        // dummy api url to filter
        val sample_url = "/services/service-test/instance-test/api"

        // create request
        val request = MockServerHttpRequest.get(sample_url).build()
        val exchange = MockServerWebExchange.from(request)

        // apply the filter to the request
        val modifyServersOpenApiFilter = spy(ModifyServersOpenApiFilter())
        modifyServersOpenApiFilter.filter(exchange, filterChain).subscribe()

        verify(modifyServersOpenApiFilter, times(0))
            .createModifyServersOpenApiInterceptor(sample_url, exchange.response, exchange.response.bufferFactory())
    }

    @Test
    fun shouldOrderToMinusOne() {
        val modifyServersOpenApiFilter = ModifyServersOpenApiFilter()
        assertEquals(modifyServersOpenApiFilter.order, -1)
    }

    @Nested
    class ModifyServersOpenApiInterceptorTest {

        private val log = LoggerFactory.getLogger(javaClass)

        private val path: String = "/services/service-test/instance-test/v3/api-docs"
        private val request: MockServerHttpRequest = MockServerHttpRequest.get(path).build()
        private val exchange: ServerWebExchange = MockServerWebExchange.from(request)
        private val modifyServersOpenApiFilter: ModifyServersOpenApiFilter = ModifyServersOpenApiFilter()

        @Test
        fun shouldRewriteBodyWhenBodyIsFluxAndResponseIsNotZipped() {
            val interceptor = modifyServersOpenApiFilter.createModifyServersOpenApiInterceptor(
                path,
                exchange.response,
                exchange.response.bufferFactory()
            )

            val bytes = "{}".encodeToByteArray()
            val body = exchange.response.bufferFactory().wrap(bytes)
            interceptor.writeWith(Flux.just(body)).subscribe()
            assertThat(
                interceptor
                    .getRewritedBody()
                    .contains("\"servers\":[{\"url\":\"/services/service-test/instance-test\",\"description\":\"added by global filter\"}]")
            ).isTrue
        }

        @Test
        fun shouldRewriteBodyWhenBodyIsFluxAndResponseIsZipped() {
            exchange.response.getHeaders().set(HttpHeaders.CONTENT_ENCODING, "gzip")
            val interceptor = modifyServersOpenApiFilter.createModifyServersOpenApiInterceptor(
                path,
                exchange.response,
                exchange.response.bufferFactory()
            )

            val bytes = zipContent()
            val body = exchange.response.bufferFactory().wrap(bytes)
            interceptor.writeWith(Flux.just(body)).subscribe()
            assertThat(
                interceptor
                    .getRewritedBody()
                    .contains("\"servers\":[{\"url\":\"/services/service-test/instance-test\",\"description\":\"added by global filter\"}]")
            ).isTrue
        }

        @Test
        fun shouldNotRewriteBodyWhenBodyIsNotFlux() {
            val interceptor = modifyServersOpenApiFilter.createModifyServersOpenApiInterceptor(
                path,
                exchange.response,
                exchange.response.bufferFactory()
            )

            val bytes = "{}".encodeToByteArray()
            val body = exchange.response.bufferFactory().wrap(bytes)
            interceptor.writeWith(Mono.just(body)).subscribe()
            assertThat(interceptor.getRewritedBody()).isEmpty()
        }

        private fun zipContent(): ByteArray {
            try {
                val byteArrayOutputStream = ByteArrayOutputStream("{}".length)
                val gzipOutputStream = GZIPOutputStream(byteArrayOutputStream)
                gzipOutputStream.write("{}".encodeToByteArray())
                gzipOutputStream.flush()
                gzipOutputStream.close()
                return byteArrayOutputStream.toByteArray()
            } catch (e: IOException) {
                log.error("Error in test when zip content during modify servers from api-doc of {}: {}", path, e.message)
            }
            return "{}".encodeToByteArray()
        }
    }
}
