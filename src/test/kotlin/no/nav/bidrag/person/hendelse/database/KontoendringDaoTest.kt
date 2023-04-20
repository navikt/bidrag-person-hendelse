package no.nav.bidrag.person.hendelse.database

import io.kotest.matchers.shouldBe
import no.nav.bidrag.person.hendelse.Teststarter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.assertj.core.api.SoftAssertions.assertSoftly

@SpringBootTest(
    classes = [Teststarter::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KontoendringDaoTest {

    @Autowired
    lateinit var kontoendringDao: KontoendringDao

    @BeforeEach
    fun initialisere() {
        kontoendringDao.deleteAll()
    }

    @Test
    fun skalLagreNyKontoendring() {

        // gitt
        var nyKontoendring = Kontoendring("Ole Brum")

        // hvis
        var lagretKontoendring = kontoendringDao.save(nyKontoendring)

        // så
        var eksisterer = kontoendringDao.findById(lagretKontoendring.id)
        assertThat(eksisterer).isPresent
    }

    @Test
    fun skalHenteKontoendringMedStatusMottatt() {

        // gitt
        kontoendringDao.save(Kontoendring("Ole Brum"))

        // hvis
        var kontoeiere = kontoendringDao.henteKontoeiere(StatusKontoendring.MOTTATT)

        // så
        assertSoftly{
            kontoeiere.size shouldBe 1
            kontoeiere.first() shouldBe "Ole Brum"
        }
    }
}