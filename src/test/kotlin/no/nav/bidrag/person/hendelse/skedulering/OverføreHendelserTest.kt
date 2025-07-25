package no.nav.bidrag.person.hendelse.skedulering

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.database.AktorDao
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.HendelsemottakDao
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.domene.Endringstype
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.exception.OverføringFeiletException
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.bisys.BisysMeldingsprodusjon
import no.nav.bidrag.person.hendelse.konfigurasjon.Testkonfig
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles(Testkonfig.PROFIL_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Teststarter::class])
class OverføreHendelserTest {
    val personidenter = listOf("12345678901", "1234567890123")

    @Autowired
    lateinit var aktorDao: AktorDao

    @Autowired
    lateinit var hendelsemottakDao: HendelsemottakDao

    lateinit var databasetjeneste: Databasetjeneste

    @Autowired
    lateinit var egenskaper: Egenskaper

    @Autowired
    lateinit var entityManager: EntityManager

    @MockK
    lateinit var meldingsprodusent: BisysMeldingsprodusjon

    lateinit var overføreHendelser: OverføreHendelser

    @BeforeEach
    fun initialisere() {
        MockKAnnotations.init(this)
        clearAllMocks()
        databasetjeneste = Databasetjeneste(aktorDao, hendelsemottakDao, egenskaper, entityManager)
        hendelsemottakDao.deleteAll()
        aktorDao.deleteAll()
        overføreHendelser = OverføreHendelser(databasetjeneste, egenskaper, meldingsprodusent)
        every { meldingsprodusent.sendeMeldinger(any(), any()) } returns 1
    }

    @Test
    @Transactional
    fun `skal sette status til OVERFØRING_FEILET dersom exception oppstår under sending`() {
        // gitt
        val hendelseid1 = "c096ca6f-9801-4543-9a44-116f4ed806ca"
        val hendelseMottattUtenforVenteperiode =
            Livshendelse(
                hendelseid1,
                Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
                Endringstype.OPPRETTET,
                personidenter,
                personidenter.first { it.length == 13 },
                LocalDateTime.now(),
            )
        val lagretHendelseVenteperiodeUtløpt = databasetjeneste.lagreHendelse(hendelseMottattUtenforVenteperiode)
        entityManager.flush()
        lagretHendelseVenteperiodeUtløpt.statustidspunkt =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong() + 5)
        val oppdatertHendelseMedUtløptVenteperiode = hendelsemottakDao.save(lagretHendelseVenteperiodeUtløpt)
        entityManager.flush()
        log.info("Lagret hendelse med statustidspunkt {}", oppdatertHendelseMedUtløptVenteperiode.statustidspunkt)

        val hendelseid2 = "38468520-70f2-40c0-b4ae-6c765c307a7d"
        val hendelseMottattInnenforVenteperiode =
            Livshendelse(
                hendelseid2,
                Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
                Endringstype.ANNULLERT,
                personidenter,
                personidenter.first { it.length == 13 },
                LocalDateTime.now(),
            )
        val lagretHendelserVenteperiodeIkkeUtløpt = databasetjeneste.lagreHendelse(hendelseMottattInnenforVenteperiode)
        entityManager.flush()
        lagretHendelserVenteperiodeIkkeUtløpt.statustidspunkt =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong() - 5)
        val oppdatertHendelseVenteperiodeIkkeUtløpt = hendelsemottakDao.save(lagretHendelserVenteperiodeIkkeUtløpt)
        entityManager.flush()
        log.info("Lagret hendelse med statustidspunkt {}", oppdatertHendelseVenteperiodeIkkeUtløpt.statustidspunkt)

        val hendelseid3 = "87925614-70f2-40c0-b4ae-6c765c308h8h"
        val hendelseMedStatusOverført =
            Livshendelse(
                hendelseid3,
                Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
                Endringstype.ANNULLERT,
                personidenter,
                personidenter.first { it.length == 13 },
                LocalDateTime.now(),
            )
        val lagretHendelseMedStatusOverført = databasetjeneste.lagreHendelse(hendelseMedStatusOverført)
        entityManager.flush()
        val oppdatertHendelseMedStatusOverført = hendelsemottakDao.save(lagretHendelseMedStatusOverført)
        entityManager.flush()
        log.info("Lagret hendelse med statustidspunkt {}", oppdatertHendelseMedStatusOverført.statustidspunkt)

        every { meldingsprodusent.sendeMeldinger(any(), any()) } throws OverføringFeiletException("auda!")

        // hvis
        overføreHendelser.overføreHendelserTilBisys()

        // så
        val meldingerTilKø = slot<List<String>>()
        verify(exactly = 1) {
            meldingsprodusent.sendeMeldinger(
                egenskaper.integrasjon.wmq.queueNameLivshendelser,
                capture(meldingerTilKø),
            )
        }
        assertThat(meldingerTilKø.captured[0]).contains(hendelseMottattUtenforVenteperiode.hendelseid)
        assertThat(
            databasetjeneste.hendelsemottakDao
                .findById(lagretHendelseVenteperiodeUtløpt.id)
                .get()
                .status,
        ).isEqualTo(Status.OVERFØRING_FEILET)
    }

    @Test
    @Transactional
    fun skalOverføreHendelserMedStatusMottattOgUtløptVentetid() {
        // gitt
        val hendelseid1 = "c096ca6f-9801-4543-9a44-116f4ed806ca"
        val hendelseMottattUtenforVenteperiode =
            Livshendelse(
                hendelseid1,
                Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
                Endringstype.OPPRETTET,
                personidenter,
                personidenter.first { it.length == 13 },
                LocalDateTime.now(),
            )
        val lagretHendelseVenteperiodeUtløpt = databasetjeneste.lagreHendelse(hendelseMottattUtenforVenteperiode)
        entityManager.flush()
        lagretHendelseVenteperiodeUtløpt.statustidspunkt =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong() + 5)
        val oppdatertHendelseMedUtløptVenteperiode = hendelsemottakDao.save(lagretHendelseVenteperiodeUtløpt)
        entityManager.flush()
        log.info("Lagret hendelse med statustidspunkt {}", oppdatertHendelseMedUtløptVenteperiode.statustidspunkt)

        val hendelseid2 = "38468520-70f2-40c0-b4ae-6c765c307a7d"
        val hendelseMottattInnenforVenteperiode =
            Livshendelse(
                hendelseid2,
                Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
                Endringstype.ANNULLERT,
                personidenter,
                personidenter.first { it.length == 13 },
                LocalDateTime.now(),
            )
        val lagretHendelserVenteperiodeIkkeUtløpt = databasetjeneste.lagreHendelse(hendelseMottattInnenforVenteperiode)
        entityManager.flush()
        lagretHendelserVenteperiodeIkkeUtløpt.statustidspunkt =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong() - 5)
        val oppdatertHendelseVenteperiodeIkkeUtløpt = hendelsemottakDao.save(lagretHendelserVenteperiodeIkkeUtløpt)
        log.info("Lagret hendelse med statustidspunkt {}", oppdatertHendelseVenteperiodeIkkeUtløpt.statustidspunkt)

        val hendelseid3 = "87925614-70f2-40c0-b4ae-6c765c308h8h"
        val hendelseMedStatusOverført =
            Livshendelse(
                hendelseid3,
                Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
                Endringstype.ANNULLERT,
                personidenter,
                personidenter.first { it.length == 13 },
                LocalDateTime.now(),
            )
        val lagretHendelseMedStatusOverført = databasetjeneste.lagreHendelse(hendelseMedStatusOverført)
        entityManager.flush()
        val oppdatertHendelseMedStatusOverført = hendelsemottakDao.save(lagretHendelseMedStatusOverført)
        entityManager.flush()
        log.info("Lagret hendelse med statustidspunkt {}", oppdatertHendelseMedStatusOverført.statustidspunkt)

        // hvis
        overføreHendelser.overføreHendelserTilBisys()

        // så
        val meldingerTilKø = slot<List<String>>()
        verify(exactly = 1) {
            meldingsprodusent.sendeMeldinger(
                egenskaper.integrasjon.wmq.queueNameLivshendelser,
                capture(meldingerTilKø),
            )
        }
        assertThat(meldingerTilKø.captured[0]).contains(hendelseMottattUtenforVenteperiode.hendelseid)
    }

    @Test
    @Transactional
    fun `skal ikke overføre flere hendelser enn maks antall om gangen`() {
        // gitt
        val hendelseid1 = "c096ca6f-9801-4543-9a44-116f4ed806ca"
        val hendelse1 =
            Livshendelse(
                hendelseid1,
                Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
                Endringstype.OPPRETTET,
                personidenter,
                personidenter.first { it.length == 13 },
                LocalDateTime.now(),
            )
        val lagretHendelse1 = databasetjeneste.lagreHendelse(hendelse1)
        entityManager.flush()
        lagretHendelse1.statustidspunkt =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong() + 5)
        hendelsemottakDao.save(lagretHendelse1)
        entityManager.flush()

        val hendelseid2 = "38468520-70f2-40c0-b4ae-6c765c307a7d"
        val hendelse2 =
            Livshendelse(
                hendelseid2,
                Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
                Endringstype.ANNULLERT,
                personidenter,
                personidenter.first { it.length == 13 },
                LocalDateTime.now(),
            )
        val lagretHendelse2 = databasetjeneste.lagreHendelse(hendelse2)
        entityManager.flush()
        lagretHendelse2.statustidspunkt =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong() + 2)
        hendelsemottakDao.save(lagretHendelse2)
        entityManager.flush()

        // hvis
        overføreHendelser.overføreHendelserTilBisys()

        // så
        val meldingerTilKø = slot<List<String>>()
        // Maks antall satt i test application.yml (egenskaper.generelt.maksAntallMeldingerSomOverfoeresTilBisysOmGangen)
        verify(exactly = 1) {
            meldingsprodusent.sendeMeldinger(
                egenskaper.integrasjon.wmq.queueNameLivshendelser,
                capture(meldingerTilKø),
            )
        }
        assertThat(meldingerTilKø.captured[0]).contains(hendelse1.hendelseid)
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(OverføreHendelser::class.java)
    }
}
