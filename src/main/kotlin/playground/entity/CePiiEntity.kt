package playground.entity

//import jakarta.persistence.Column
//import jakarta.persistence.Entity
//import jakarta.persistence.Id
//import jakarta.persistence.Table
import playground.customer.contract.avro.customerprofilesnapshot.v2.CustomerProfileSnapshot
import java.time.Instant


//@Entity
//@Table(name = "ce_pii")
data class CePiiEntity(

//    @Id
//    @Column(nullable = false, updatable = false)
    val customerId: String,

//    @Column(nullable = false)
    val externalId: String,

    val fullName: String?,
    val emailAddress: String?,
    val phoneNumber: String?,
    val latitude: Double,
    val longitude: Double,
    val joinedAt: Instant,
    val snapshot: CustomerProfileSnapshot,

    ) {
//    companion object {
//        fun from(snapshot: CustomerProfileSnapshot,
////                 location: ResidentialLocation? = null
//        ) = CePiiEntity(
//                customerId = snapshot.customerId.toString(),
//                externalId = snapshot.externalId!!.toString(),
//                fullName = snapshot.fullName.toString(),
//                emailAddress = snapshot.email.toString(),
//                phoneNumber = snapshot.phoneNumber?.toString(),
////                latitude = location?.latitude ?: 0.0,
////                longitude = location?.longitude ?: 0.0,
//                latitude = 0.0,
//                longitude = 0.0,
//                joinedAt = snapshot.createdAt!!
//            )
//        fun from(customerProfile: CustomerProfile,
//                 location: ResidentialLocation? = null
//        ) = CePiiEntity(
//                customerId = customerProfile.id,
//                externalId = customerProfile.externalId,
//                fullName = customerProfile.fullName,
//                emailAddress = customerProfile.email,
//                phoneNumber = customerProfile.phoneNumber?.toJson(),
//                latitude = location?.latitude ?: 0.0,
//                longitude = location?.longitude ?: 0.0,
//                joinedAt = customerProfile.createdAt
//            )
//    }
}
