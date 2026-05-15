package no.nav.bidrag.person.hendelse.database

import io.kotest.matchers.shouldBe
import jakarta.transaction.Transactional
import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.domene.Endringstype
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.domene.Livshendelse.Opplysningstype
import no.nav.bidrag.person.hendelse.konfigurasjon.Testkonfig.Companion.PROFIL_TEST
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles(PROFIL_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Teststarter::class])
class DatabasetjenesteTest {
    private val personidenter = listOf("12345678901", "1234567890123")

    @Autowired
    lateinit var hendelsemottakDao: HendelsemottakDao

    @Autowired
    lateinit var databasetjeneste: Databasetjeneste

    @Nested
    open inner class Hendelsemottak {
        @BeforeEach
        fun initialisere() {
            hendelsemottakDao.deleteAll()
        }

        @Test
        @Transactional
        open fun skalKansellereTidligereOgNyHendelseVedAnnulleringDersomTidligereHendelseIkkeErOverført() {
            // gitt
            val hendelseidOpprinneligHendelse = "c096ca6f-9801-4543-9a44-116f4ed806ce"
            val opprinneligHendelse =
                Livshendelse(
                    hendelseidOpprinneligHendelse,
                    Opplysningstype.BOSTEDSADRESSE_V1,
                    Endringstype.OPPRETTET,
                    personidenter,
                    personidenter.first { it.length == 13 },
                    LocalDateTime.now(),
                )
            val lagretOpprinneligHendelse = databasetjeneste.lagreHendelse(opprinneligHendelse)

            val hendelseidAnnulleringshendelse = "38468520-70f2-40c0-b4ae-6c765c307a7d"
            val annulleringAvOpprinneligHendelse =
                Livshendelse(
                    hendelseidAnnulleringshendelse,
                    Opplysningstype.BOSTEDSADRESSE_V1,
                    Endringstype.ANNULLERT,
                    personidenter,
                    personidenter.first { it.length == 13 },
                    LocalDateTime.now(),
                    hendelseidOpprinneligHendelse,
                )

            // hvis
            val lagretAnnulleringAvOpprinneligHendelse =
                databasetjeneste.lagreHendelse(annulleringAvOpprinneligHendelse)

            // så
            val lagretOpprinneligHendelseEtterKansellering = hendelsemottakDao.findById(lagretOpprinneligHendelse.id!!)
            val lagretNyHendelseEtterKansellering =
                hendelsemottakDao.findById(lagretAnnulleringAvOpprinneligHendelse.id!!)

            assertSoftly {
                lagretOpprinneligHendelseEtterKansellering.isPresent
                lagretOpprinneligHendelseEtterKansellering.get().status shouldBe Status.KANSELLERT
                lagretNyHendelseEtterKansellering.isPresent
                lagretNyHendelseEtterKansellering.get().status shouldBe Status.KANSELLERT
            }
        }

        @Test
        fun `skal kansellere opphør av bostedsadresse`() {
            // gitt
            val hendelseid = "c096ca6f-9801-4543-9a44-116f4ed806ce"
            val hendelse =
                Livshendelse(
                    hendelseid,
                    Opplysningstype.BOSTEDSADRESSE_V1,
                    Endringstype.OPPHOERT,
                    personidenter,
                    personidenter.first { it.length == 13 },
                    LocalDateTime.now(),
                )

            // hvis
            val lagretHendelse = databasetjeneste.lagreHendelse(hendelse)

            // så
            assertSoftly {
                lagretHendelse.status shouldBe Status.KANSELLERT
            }
        }

        @Test
        fun `skal håndtere høyt antall personidenter`() {
            // gitt
            val langRekkePersonidenter =
                listOf(
                    "12345678910",
                    "12345678911",
                    "12345678912",
                    "12345678913",
                    "12345678914",
                    "12345678915",
                    "12345678916",
                    "12345678917",
                    "12345678918",
                    "12345678919",
                    "12345678910",
                    "2345678910123",
                    "22345678910",
                    "22345678911",
                    "22345678912",
                    "22345678913",
                    "22345678914",
                    "22345678915",
                    "32345678913",
                    "32345678914",
                    "32345678915",
                )
            val hendelseid = "c096ca6f-9801-4543-9a44-116f4ed806ce"
            val hendelse =
                Livshendelse(
                    hendelseid,
                    Opplysningstype.BOSTEDSADRESSE_V1,
                    Endringstype.OPPHOERT,
                    langRekkePersonidenter,
                    personidenter.first { it.length == 13 },
                    LocalDateTime.now(),
                )

            // hvis
            val lagretHendelse = databasetjeneste.lagreHendelse(hendelse)

            // så
            assertSoftly {
                lagretHendelse.status shouldBe Status.KANSELLERT
            }
        }

        @Test
        @Transactional
        open fun tidligereHendelseidFinnesIkkeIDatabasen() {
            // gitt
            val hendelseidOpprinneligHendelse = "c096ca6f-9801-4543-9a44-116f4ed806ce"

            val hendelseidAnnulleringshendelse = "38468520-70f2-40c0-b4ae-6c765c307a7d"
            val annulleringAvOpprinneligHendelse =
                Livshendelse(
                    hendelseidAnnulleringshendelse,
                    Opplysningstype.BOSTEDSADRESSE_V1,
                    Endringstype.ANNULLERT,
                    personidenter,
                    personidenter.first { it.length == 13 },
                    LocalDateTime.now(),
                    hendelseidOpprinneligHendelse,
                )

            // hvis
            val lagretAnnulleringAvOpprinneligHendelse =
                databasetjeneste.lagreHendelse(annulleringAvOpprinneligHendelse)

            // så
            val lagretNyHendelseEtterKansellering =
                hendelsemottakDao.findById(lagretAnnulleringAvOpprinneligHendelse.id!!)

            assertSoftly {
                lagretNyHendelseEtterKansellering.isPresent
                lagretNyHendelseEtterKansellering.get().status shouldBe Status.MOTTATT
            }
        }
    }
}
