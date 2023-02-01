import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.csv.CsvGenerator
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema

fun main() {
    println("Hello World!")


    val header1 = "\"Id\",\"Name\",\"Born City\",\"Gender\",\"Born City\",\"completion_time\",\"test./test\""
    val header2 = "Id,Name,Born City,Gender,\"Born City\",completion_time,test"
    val bootstrapSchema = CsvSchema.emptySchema().withHeader()
    val mapper = CsvMapper.builder()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
        .enable(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
    val content = object {}.javaClass.getResourceAsStream("/items.csv")

    //    val content =
//        """
//        Id,Name,Born City,Gender,"Born City",completion_time
//        0,"Tse, Dan",30,M,"Hong Kong",123123
//        1,"Ho, Crystal",25,F,"Hong Kong",53443534
//        """
//        .trimIndent()

    // Get header row and check first?
//    val reader1 = content.bufferedReader()
//
//    println(reader1.readLine())
//    reader1.

//    mapper.reader().readValues()

    // println(returnNull().also { println(it ?: "also") } ?: "it's null!")

//    val reader = content?.bufferedReader()
//    val header = reader?.readLine()
//    val readValues1: MappingIterator<Item> = mapper.readerFor(Item::class.java)
//        .with(CsvSchema.emptySchema())
//        .readValues(reader)
//    val readAll: List<Item> = readValues1.readAll()
//    readAll.forEach {
//        println(it.id)
//        println(it.name)
//        println(it.bornCity)
//        println(it.completionTime)
//    }



//    content?.bufferedReader().use {
//        val header = it?.readLine()
//        println("Header: $header");
//        when(header) {
//            header1 -> {
//                println("Header 1 here")
//                    val readValues1: MappingIterator<Item> = mapper.readerFor(Item::class.java)
//                        .with(CsvSchema.emptySchema())
//                        .readValues(it)
//                    val readAll: List<Item> = readValues1.readAll()
//                    readAll.forEach {
//                        println(it.id)
//                        println(it.name)
//                        println(it.bornCity)
//                        println(it.completionTime)
//                    }
//
//            }
//            header2 -> {
//                println("Header 2 here")
//            }
//            else -> {
//                println("Not supported")
//            }
//        }
//    }


//    val readValues: MappingIterator<String> = mapper.readerFor(String::class.java)
//        .with(mapper.typedSchemaFor(String::class.java).withHeader())
//        .readValues(content?.bufferedReader())


//    val readValues2: MappingIterator<Item2> = mapper.readerFor(Item2::class.java)
//        .with(CsvSchema.emptySchema().withHeader())
//        .readValues(content.bufferedReader())

//    val readAll: List<Item> = readValues.readAll()

//    readAll.forEach {
//        println(it.id)
//        println(it.name)
//        println(it.bornCity)
//        println(it.completionTime)
//    }

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
//    println("Program arguments: ${args.joinToString()}")
}

fun returnNull(): Unit? {
    return null
}