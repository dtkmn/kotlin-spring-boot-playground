package playground.common.restclient

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.DefaultResponseErrorHandler
import playground.common.exception.MoxRestClientException
import playground.common.exception.error.DragonErrorResponse

@Component
class DragonErrorResponseErrorHandler(private val defaultObjectMapper: ObjectMapper) : DefaultResponseErrorHandler() {

    override fun getResponseBody(response: ClientHttpResponse): ByteArray {
        val body = super.getResponseBody(response)
        try {
            val error = defaultObjectMapper.readValue<DragonErrorResponse>(body)
            throw MoxRestClientException(HttpStatus.valueOf(response.statusCode.value()), error.errorCode, error.debugMessage ?: "", error.validationErrors)
        } catch (ex: MoxRestClientException) {
            throw ex
        } catch (ex: Exception) {
            return body
        }
        return body
    }
}
