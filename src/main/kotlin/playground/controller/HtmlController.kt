package playground.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import playground.customer.contract.avro.customerprofilesnapshot.v2.*
import playground.entity.CePiiEntity
import playground.publisher.EventPublisher
import playground.repository.CePiiRepository
import java.time.Instant
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/test")
class HtmlController(
    private val cePiiRepository: CePiiRepository,
    private val eventPublisher: EventPublisher,
) {

    @GetMapping("/")
    fun blog(): String {
        return "This is from Kotlin Playground microservice!"
    }

    @GetMapping("/listall")
    fun listAllEntity(): String {
        cePiiRepository.findAll().forEach {
            println(it.customerId)
            println(it.emailAddress)
        }
        return "This is from Kotlin Playground microservice!"
    }

    @PostMapping("/create")
    fun createEntity(): String {
        val cePiiEntity = cePiiRepository.save(
            CePiiEntity(
                customerId = UUID.randomUUID().toString(),
                externalId = "12312313",
                latitude = 0.0,
                longitude = 0.0,
                joinedAt = Instant.now(),
                emailAddress = "test@test.com",
                fullName = "Testmate",
                phoneNumber = "0433322333"
            )
        )
        return "Record created with customer id: " + cePiiEntity.customerId
    }

    @PostMapping("/publish")
    fun publishCustomer(): String {
        val customerProfileSnapshot = CustomerProfileSnapshot()
            .also {
                with(it) {
                    customerId = UUID.randomUUID().toString()
                    externalId = "some_external_id"
                    status = Status.ACTIVE
                    extendedStatus = ExtendedStatus.ACTIVE
                    firstName = "Leia"
                    lastName = "Organa"
                    fullName = "Leia Organa"
                    aliasName = null
                    nickname = "Princess Leia"
                    phoneNumber = PhoneNumber().apply {
                        number = "123456"
                        countryCode = "+852"
                        countryCodeISO2 = "HK"
                        verifiedAt = Instant.parse("2019-07-04T17:20:30.321Z")
                    }
                    email = "princess.leia@rebels.hk"
                    gender = Gender.FEMALE
                    dateOfBirth = LocalDate.of(1879, 12, 18)
                    placeOfBirth = "Hong Kong"
                    countryOfBirth = "HK"
                    countryOfResidence = null
                    idType = IdType.PERM_ID
                    idIssuingCountry = "HK"
                    idCountryOfBirth = null
                    identificationCardNumber = "M123456(7)"
                    nationalities = listOf("HK")
                    taxInfoList = emptyList() // to be used in combination with with*Tax* functions
                    occupation = Occupation().apply {
                        avgMonthlyIncome = null
                        status = "OCRT"
                        sector = "RS16"
                        subtype = null
                    }
                    usCitizenOrGreenCardHolder = false
                    usSocialSecurityNumber = null
                    tncVersion = "1.0"
                    creditConsent = false
                    craRating = CraRating.A
                    cddReviewStatus = null
                    cddReviewDate = LocalDate.of(2022, 12, 18)
                    cddNextReviewDate = LocalDate.of(2022, 12, 18)
                    pepStatus = NameScreeningStatus.FALSE
                    amStatus = NameScreeningStatus.FALSE
                    onboardingStp = null
                    hnwStatus = null
                    sofOverLimit = null
                    aumOver2mStatus = null
                    aumOver5mStatus = null
                    sanctionsStatus = NameScreeningStatus.FALSE
                    ddStatus = null
                    privacyPolicyVersion = "pp-v2.0"
                    accountPurposeInstalments = false
                    accountPurposeInternationalPayments = false
                    accountPurposeSavings = false
                    accountPurposeTransactions = false
                    scanId = "test-scan-id"
                    submittedAt = Instant.parse("2020-07-04T17:22:30.321Z")
                    onboardingDate = LocalDate.of(2022, 4, 29)
                    offboardedAt = null
                    onboardedAt = Instant.parse("2021-07-04T17:22:30.321Z")
                    deniedAt = null
                    deletedAt = Instant.parse("2020-07-04T17:13:30.321Z")
                    staff = false
                    ntbr = false
                    potentialTaxResidencies = null
                    recalcitrant = false
                    unresponsive = false
                    emailVerified = false
                    emailInvalid = false
                    tags = emptyList()
                    mailingAddress = null
                    residentialAddress = CustomerAddress().apply {
                        id = "some_id"
                        apartment = null
                        streetName = null
                        unit = null
                        addressLine1 = null
                        addressLine2 = null
                        addressLine3 = null
                        addressPurpose = AddressPurpose.RESIDENTIAL
                        block = null
                        city = null
                        country = null
                        createdAt = Instant.parse("2019-07-04T17:20:30.321Z")
                        updatedAt = null
                        flat = null
                        floor = null
                        building = null
                        fullAddress = null
                        postalCode = "050336"
                        rawAddress = null
                    }
                    createdAt = Instant.parse("2020-07-04T17:13:30.321Z")
                    updatedAt = Instant.parse("2020-07-04T17:23:30.321Z")
                    denyCode = DenyCode.ONBO_DENY_CDD
                    denyCodeV2 = DenyCodeV2.ONBO_DENY_CDD
                    denyReason = ""
                    formerPep = null
                    unionMember = false
                    unionStatus = UnionStatus.ACTIVE
                    controllingDirector = null
                    employment = null
                    applicationId = null
                    nric = null
                    maritalStatus = null
                    residentialStatus = null
                    noa = null
                    cpfHistory = emptyList()
                    passType = null
                    passStatus = null
                    passExpiryDate = null
                    newAcceptedConsents = emptyList()
                    referralCode = "ABCHA74"
                }
            }
        eventPublisher.publishCustomerProfileSnapshot(customerProfileSnapshot)
        return "New customer published!!"
    }

}
