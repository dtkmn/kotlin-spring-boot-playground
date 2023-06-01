package playground.common.restclient

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import playground.common.exception.ValidationException
import playground.common.exception.error.ERR_SYS_NUMERIC_TIMESTAMP
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import playground.common.restclient.logging.LOG_EVENT_CREATING_REST_TEMPLATE_BUILDER
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.function.BiFunction
import java.util.function.Function

@Configuration
@Import(DragonErrorResponseErrorHandler::class, MoxRestClientLoggingInterceptor::class)
class RestClientConfiguration {
    private val log = LoggerFactory.getLogger(RestClientConfiguration::class.java)

    @Bean
    @Primary
    fun defaultObjectMapper(): ObjectMapper = jacksonObjectMapper().apply {
        // Added so the timestamps are serialized in human readable format
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        // nanos is the default, setting explicitly for better visibility
        configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, true)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, true)

        registerModule(
            JavaTimeModule().apply {
                addDeserializer(Instant::class.java, instantDeserializer())
                addDeserializer(OffsetDateTime::class.java, offsetDateTimeDeserializer())
                addDeserializer(ZonedDateTime::class.java, zonedDateTimeDeserializer())
                // TODO figure out how to override or set lenient flag
                // addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer())
            }
        )
    }

    // only allow string timestamps
    fun instantDeserializer(): InstantDeserializer<Instant> {
        return object : InstantDeserializer<Instant>(
            Instant::class.java,
            DateTimeFormatter.ISO_INSTANT,
            Function<TemporalAccessor, Instant> { temporal: TemporalAccessor? -> Instant.from(temporal) },
            Function<FromIntegerArguments, Instant> { failOnNumericTimestamp() },
            Function<FromDecimalArguments, Instant> { failOnNumericTimestamp() },
            null,
            true
        ) {}
    }

    fun offsetDateTimeDeserializer(): InstantDeserializer<OffsetDateTime> {
        return object : InstantDeserializer<OffsetDateTime>(
            OffsetDateTime::class.java, DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            Function { temporal: TemporalAccessor? -> OffsetDateTime.from(temporal) },
            Function<FromIntegerArguments, OffsetDateTime> { failOnNumericTimestamp() },
            Function<FromDecimalArguments, OffsetDateTime> { failOnNumericTimestamp() },
            BiFunction { d: OffsetDateTime, z: ZoneId -> d.withOffsetSameInstant(z.rules.getOffset(d.toLocalDateTime())) },
            true
        ) {}
    }

    fun zonedDateTimeDeserializer(): InstantDeserializer<ZonedDateTime> {
        return object : InstantDeserializer<ZonedDateTime>(
            ZonedDateTime::class.java, DateTimeFormatter.ISO_ZONED_DATE_TIME,
            Function { temporal: TemporalAccessor? -> ZonedDateTime.from(temporal) },
            Function<FromIntegerArguments, ZonedDateTime> { failOnNumericTimestamp() },
            Function<FromDecimalArguments, ZonedDateTime> { failOnNumericTimestamp() },
            BiFunction { obj: ZonedDateTime, zone: ZoneId? -> obj.withZoneSameInstant(zone) },
            false // keep zero offset and Z separate since zones explicitly supported
            // keep zero offset and Z separate since zones explicitly supported
        ) {}
    }

    private fun failOnNumericTimestamp(): Nothing =
        throw ValidationException("Numeric timestamps are not allowed.", ERR_SYS_NUMERIC_TIMESTAMP)

    @Bean
    fun restTemplateBuilder(
        defaultObjectMapper: ObjectMapper,
        @Value("\${HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE:5}") maxConnectionsPerRoute: Int,
        @Value("\${HTTP_CLIENT_MAX_CONNECTIONS_TOTAL:10}") maxConnectionsTotal: Int,
        @Value("\${dragon.rest-client.error-response-handler.enabled:false}") moxRestClientHandlerEnabled: Boolean,
        moxRestClientLoggingInterceptor: MoxRestClientLoggingInterceptor,
        dragonErrorResponseErrorHandler: DragonErrorResponseErrorHandler
    ): RestTemplateBuilder {
        log.info(
            "Creating RestTemplateBuilder with maxConnections=$maxConnectionsPerRoute and total=$maxConnectionsTotal",
            kv(MDC_KEY_LOG_EVENT, LOG_EVENT_CREATING_REST_TEMPLATE_BUILDER)
        )

        return RestTemplateBuilder()
            .withMoxConnectionPool(maxConnectionsPerRoute, maxConnectionsTotal)
            .messageConverters(MappingJackson2HttpMessageConverter(defaultObjectMapper), FormHttpMessageConverter())
            .additionalInterceptors(moxRestClientLoggingInterceptor)
            .let {
                if (moxRestClientHandlerEnabled) {
                    it.errorHandler(dragonErrorResponseErrorHandler)
                } else {
                    it
                }
            }
    }
}
