package no.nav.bidrag.person.hendelse

import no.nav.bidrag.person.hendelse.konfigurasjon.ApplicationConfig
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Import

@Import(ApplicationConfig::class)
class DevLauncher

fun main(args: Array<String>) {
    val app = SpringApplicationBuilder(DevLauncher::class.java)
        .profiles("dev")
    app.run(*args)
}
