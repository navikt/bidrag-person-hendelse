package no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.database.Aktor
import no.nav.bidrag.person.hendelse.database.AktorDao
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.HendelsemottakDao
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.konfigurasjon.Testkonfig
import no.nav.bidrag.person.hendelse.testdata.TeststøtteMeldingsmottak
import no.nav.bidrag.person.hendelse.testdata.generereIdenter
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@ActiveProfiles(Testkonfig.PROFIL_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Teststarter::class])
class BidragKafkaMeldingsprodusentTest {
    @MockK
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    lateinit var aktorDao: AktorDao

    @Autowired
    lateinit var hendelsemottakDao: HendelsemottakDao

    @Autowired
    lateinit var databasetjeneste: Databasetjeneste

    @Autowired
    lateinit var teststøtteMeldingsmottak: TeststøtteMeldingsmottak

    @Autowired
    lateinit var entityManager: EntityManager

    lateinit var bidragKafkaMeldingsprodusent: BidragKafkaMeldingsprodusent

    @BeforeEach
    internal fun oppsett() {
        MockKAnnotations.init(this)
        clearAllMocks()
        bidragKafkaMeldingsprodusent = BidragKafkaMeldingsprodusent(kafkaTemplate, databasetjeneste, entityManager, ObjectMapper())
        hendelsemottakDao.deleteAll()
        aktorDao.deleteAll()
    }

    @Test
    @Transactional
    fun `skal produsere meldinger på riktig format`() {
        // gitt
        val aktørid = "123"
        val completeableFuture: CompletableFuture<SendResult<String, String>> = mockk(relaxed = true)
        every { kafkaTemplate.send(any(), any(), any()) } returns completeableFuture

        // hvis
        bidragKafkaMeldingsprodusent.publisereEndringsmelding(aktørid, setOf("875", aktørid))

        // så
        val melding = slot<String>()
        verify(exactly = 1) {
            kafkaTemplate.send(BidragKafkaMeldingsprodusent.BIDRAG_PERSONHENDELSE_TOPIC, aktørid, capture(melding))
        }

        val streng = "{\"aktørid\":\"123\",\"personidenter\":[\"875\",\"123\"],\"endringer\":[{\"opplysningstype\":\"UKJENT\"}]}"

        Assertions.assertThat(melding.captured).isEqualTo(streng)
    }

    @Test
    @Transactional
    fun `skal oppdatere status på hendelse etter publisering`() {
        // gitt
        val personidenter = generereIdenter()
        val aktørid = personidenter.find { it.length == 13 }!!
        val hendelsemottak = teststøtteMeldingsmottak.oppretteOgLagreHendelsemottak(personidenter.toList(), Status.OVERFØRT)

        val completeableFuture: CompletableFuture<SendResult<String, String>> = mockk(relaxed = true)
        every { kafkaTemplate.send(any(), any(), any()) } returns completeableFuture

        // hvis
        bidragKafkaMeldingsprodusent.publisereEndringsmelding(aktørid, setOf("875", aktørid))

        // så
        verify(exactly = 1) {
            kafkaTemplate.send(BidragKafkaMeldingsprodusent.BIDRAG_PERSONHENDELSE_TOPIC, aktørid, any())
        }

        val aktør = aktorDao.findByAktorid(aktørid)
        assertSoftly {
            aktør.isPresent
            aktør.get().publisert shouldNotBe null
        }

        val oppdatertHendelsemottak = hendelsemottakDao.findById(hendelsemottak.id)
        assertSoftly {
            oppdatertHendelsemottak.isPresent
            oppdatertHendelsemottak.get().status shouldBe Status.PUBLISERT
            oppdatertHendelsemottak.get().statustidspunkt shouldBeAfter LocalDateTime.now().minusSeconds(5)
        }
    }

    @Test
    @Transactional
    fun `skal oppdatere publiseringstidspunkt til aktør uten hendelse`() {
        // gitt
        val aktørid = "1234"
        aktorDao.save(Aktor(aktørid))

        val completeableFuture: CompletableFuture<SendResult<String, String>> = mockk(relaxed = true)
        every { kafkaTemplate.send(any(), any(), any()) } returns completeableFuture

        // hvis
        bidragKafkaMeldingsprodusent.publisereEndringsmelding(aktørid, setOf("875", aktørid))

        // så
        verify(exactly = 1) {
            kafkaTemplate.send(BidragKafkaMeldingsprodusent.BIDRAG_PERSONHENDELSE_TOPIC, aktørid, any())
        }

        val aktør = aktorDao.findByAktorid(aktørid)
        assertSoftly {
            aktør.isPresent
            aktør.get().publisert shouldNotBe null
        }
    }
}
