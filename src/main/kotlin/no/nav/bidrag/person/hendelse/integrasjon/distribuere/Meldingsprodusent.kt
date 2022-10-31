package no.nav.bidrag.person.hendelse.integrasjon.distribuere

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Component

@Component
class Meldingsprodusent(private val jmsTemplate: JmsTemplate) {

    fun sendeMelding(mottakerkoe: String, melding: String) {
        secureLogger.info("Sender melding til {} med innhold: {}", mottakerkoe, melding)
        try {
            jmsTemplate.send(mottakerkoe) { s -> s.createTextMessage(melding) }
        } catch(e:Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(Meldingsprodusent::class.java)
        private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
