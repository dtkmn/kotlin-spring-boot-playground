package playground.common.auth

import org.springframework.security.oauth2.jwt.Jwt

sealed class MoxPrincipal(val subject: String) {
    abstract val customerId: String?

    data class MoxCustomer(val idToken: Jwt) : MoxPrincipal(idToken.subject) {
        val id: String = subject

        override val customerId: String? = id
        val acrLevel = AcrLevel.fromStringValue(idToken.getClaimAsString("acr"))
        val amr = idToken.getClaimAsString("amr")
        val transactionId = idToken.getClaimAsString("transactionId")
    }

    data class MoxStaff(
        val staffIdToken: Jwt,
        val customerIdToken: Jwt? = null
    ) : MoxPrincipal(staffIdToken.getClaimAsString("name")) {
        val name: String = staffIdToken.getClaimAsString("name")
        val email: String = staffIdToken.getClaimAsString("email")
        val roles: List<String> = staffIdToken.getClaimAsStringList("roles") ?: emptyList()
        val impersonating: MoxCustomer? = customerIdToken?.let { MoxCustomer(it) }

        override val customerId: String? = impersonating?.id
    }

    class UnboundDevice : MoxPrincipal(SUBJECT) {
        companion object {
            const val SUBJECT = "unbounddevice"
        }

        override val customerId: String? = null
    }
}
