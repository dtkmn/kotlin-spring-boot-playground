package playground.processor

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import playground.customer.contract.avro.customerprofilesnapshot.v2.CustomerProfileSnapshot

@Component
class CustomerProfileSnapshotProcessor(
//    @Value("\${dragon.messaging.customer-profile.consumer-group-id}") private val kafkaConsumerGroupId: String,
//    private val idempotencyProvider: IdempotencyProvider,
//    private val geocodingService: GeocodingService,
//    private val repository: CePiiRepository,
//    private val publishingInFinalPhase: PublishingInFinalPhase
) {

//    private val log = logger()

    @KafkaListener(
        id = "\${playground.messaging.customer-profile.consumer-group-id}",
        clientIdPrefix = "\${playground.messaging.customer-profile.id-prefix}",
        topics = ["\${playground.messaging.customer-profile.topic}"],
        concurrency = "\${playground.messaging.customer-profile.listener-count}",
        autoStartup = "\${playground.messaging.customer-profile.enabled}"
    )
    @Throws(Exception::class)
    fun process(
        @Payload snapshot: CustomerProfileSnapshot,
//        @Header(name = MoxHeaders.IDEMPOTENCY_KEY) idempotencyKey: String
    ) {
//        if (snapshot.status != Status.ACTIVE || snapshot.externalId == null || snapshot.createdAt == null) return
//
//        idempotencyProvider.runIdempotentProcess(
//            idempotencyKey = kafkaConsumerGroupId + idempotencyKey,
//            processId = javaClass.name,
//            lockTimeout = Duration.ofMillis(100),
//            settings = arrayOf(publishingInFinalPhase)
//        ) { ctx ->
//            ctx.validateProcessInputs(generateUniquePayloadMap(snapshot))
        println("Processing customer profile snapshot with customerId:  $snapshot")
//                "Processing customer profile snapshot: {}",
//                listOf(
//                    kv("customerID", snapshot.customerId),
//                    kv("externalID", snapshot.externalId!!),
//                    kv("status", snapshot.status),
//                )
//            )

//            log.info(
//                "Processing customer profile snapshot: {}",
//                listOf(
//                    kv("customerID", snapshot.customerId),
//                    kv("externalID", snapshot.externalId!!),
//                    kv("status", snapshot.status),
//                )
//            )
//            val postCode = snapshot.residentialAddress?.postalCode
//            val location = postCode?.let { geocodingService.getLocationByPostalCode(it) }

//            ctx.phase("save-pii-in-db") {
//                val entity = CePiiEntity.from(snapshot, location)
//                log.info(
//                    "Saving PII entity into database: {}",
//                    listOf(
//                        kv("customerID", entity.customerId),
//                        kv("externalID", entity.externalId)
//                    )
//                )
//                repository.save(entity)
//            }
//
//            log.info(
//                "Publishing customer snapshot",
//                listOf(
//                    kv("customerID", snapshot.customerId),
//                    kv("externalID", snapshot.externalId ?: "")
//                )
//            )
//            ctx.publish(
//                ceCustomerSnapshotTopic,
//                snapshot.customerId,
//                CeCustomerSnapshot(
//                    customerId = snapshot.customerId,
//                    externalId = snapshot.externalId!!,
//                    residentiallocation = location
//                ),
//                metadata = MessageMetadata(
//                    customerId = snapshot.customerId,
//                    idempotencyKey = ctx.idempotencyKey,
//                    timestamp = Clock.systemUTC().instant()
//                )
//            )
//        }
    }

//    fun generateUniquePayloadMap(payload: CustomerProfileSnapshot): Map<String, Any?> {
//        return mapOf(
//            "customerId" to payload.customerId,
//            "updatedAt" to payload.updatedAt
//        )
//    }
}
