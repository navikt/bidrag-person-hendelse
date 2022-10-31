package no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("wmq")
data class Wmq(
    var host: String,
    val port: Int,
    val channel: String,
    val queueManager: String,
    val username: String,
    val password: String,
    val timeout: Int,
    val applicationName: String,
    val queueNameLivshendelser: String
)
