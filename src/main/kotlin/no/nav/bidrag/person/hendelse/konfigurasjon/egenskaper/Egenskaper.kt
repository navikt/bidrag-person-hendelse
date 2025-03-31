package no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.ConfigurationProperties

val hendelseOjectmapper =
    ObjectMapper()
        .findAndRegisterModules()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

@ConfigurationProperties(prefix = "egenskaper")
data class Egenskaper(
    val generelt: Generelt,
    val integrasjon: Integrasjon,
)

@ConfigurationProperties("generelt")
data class Generelt(
    val antallMinutterForsinketVideresending: Int = 120,
    val antallDagerLevetidForUtgaatteHendelser: Int = 30,
    val antallTimerSidenForrigePublisering: Int = 8,
    val maksAntallMeldingerSomOverfoeresTilBisysOmGangen: Int = 6500,
    val maksAntallMeldingerSomSendesTilBidragTopicOmGangen: Int = 2000,
    val bolkstoerrelseVedSletting: Int = 65000,
)

@ConfigurationProperties("egenskaper.integrasjon")
data class Integrasjon(
    val wmq: Wmq,
    val bidragPerson: BidragPerson,
)

@ConfigurationProperties("egenskaper.integrasjon.wmq")
data class Wmq(
    var host: String,
    val port: Int,
    val channel: String,
    val queueManager: String,
    val username: String,
    val password: String,
    val timeout: Int,
    val applicationName: String,
    val queueNameLivshendelser: String,
)

@ConfigurationProperties("egenskaper.integrasjon.bidrag-person")
data class BidragPerson(
    val url: String,
)
