package no.nav.bidrag.person.hendelse

import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Wmq
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

@SpringBootApplication
@EnableConfigurationProperties(Wmq::class)
open class Launcher

fun main(args: Array<String>) {
    SpringApplication.run(Launcher::class.java, *args)
}
