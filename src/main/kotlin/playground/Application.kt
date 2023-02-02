package playground

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

//fun helloWorld() {
//    println("Hello World!")
//    val header1 = "\"Id\",\"Name\",\"Born City\",\"Gender\",\"Born City\",\"completion_time\",\"test./test\""
//    val header2 = "Id,Name,Born City,Gender,\"Born City\",completion_time,test"
//    val bootstrapSchema = CsvSchema.emptySchema().withHeader()
//    val mapper = CsvMapper.builder()
//        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
//        .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
//        .enable(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
//        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
//        .build()
//    val content = object {}.javaClass.getResourceAsStream("/items.csv")
//}
//
//fun returnNull(): Unit? {
//    return null
//}