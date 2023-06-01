package playground.common.auth

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

class MoxSecurity {
    companion object {
        val authentication: Authentication? get() = SecurityContextHolder.getContext().authentication

        val isAuthenticated: Boolean get() = authentication?.isAuthenticated ?: false

        val isBasicAuth: Boolean get() = authentication?.let { it is UsernamePasswordAuthenticationToken } ?: false

        val moxAuthentication: MoxAuthentication? get() = authentication.convertOrNull()

        val principal: MoxPrincipal? get() = moxAuthentication?.principal

        val customerId: String? get() = principal?.customerId

        val staff: MoxPrincipal.MoxStaff? get() = principal.convertOrNull()

        val isUnboundedDevice: Boolean get() = principal.convertOrNull<MoxPrincipal, MoxPrincipal.UnboundDevice>() != null

        val isCustomer: Boolean get() = moxAuthentication?.let { it.principal is MoxPrincipal.MoxCustomer } ?: false

        val isStaff: Boolean get() = staff != null

        // TODO: will be MoxServiceAuthentication eventually
        val isService: Boolean get() = authentication is MoxAnonymousAuthentication

        val acrLevel: AcrLevel? get() = moxAuthentication?.principal.convertOrNull<MoxPrincipal, MoxPrincipal.MoxCustomer?>()?.acrLevel

        fun isAtLeastAcrLevel(other: AcrLevel): Boolean = acrLevel?.isAtLeastAcrLevel(other) ?: false

        val amr: String? get() = moxAuthentication?.principal
            .convertOrNull<MoxPrincipal, MoxPrincipal.MoxCustomer?>()?.amr

        val transactionId: String? get() = moxAuthentication?.principal
            .convertOrNull<MoxPrincipal, MoxPrincipal.MoxCustomer?>()?.transactionId

        fun isAcr2forTransactionId(transactionId: String): Boolean = Companion.transactionId == transactionId && acrLevel == AcrLevel.ACR2

        private inline fun <reified T, reified R> T?.convertOrNull(): R? = this?.takeIf { it is R }?.let { it as R }
    }
}
