package no.nav.bidrag.person.hendelse

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@EnableAspectJAutoProxy
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class PersonHendelseLocal

fun main(args: Array<String>) {
    val app = SpringApplication(Starter::class.java)
    app.setAdditionalProfiles("lokal-nais", "nais", "Lokal-nais-secrets", "lokal-postgres")
    app.run(*args)
}
