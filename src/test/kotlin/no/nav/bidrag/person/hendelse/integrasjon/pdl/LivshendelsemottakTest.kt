package no.nav.bidrag.person.hendelse.integrasjon.pdl

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.domene.Livshendelse.Opplysningstype
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.bostedsadresse.Bostedsadresse
import no.nav.person.pdl.leesah.common.adresse.Vegadresse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.foedsel.Foedsel
import no.nav.person.pdl.leesah.foedselsdato.Foedselsdato
import no.nav.person.pdl.leesah.folkeregisteridentifikator.Folkeregisteridentifikator
import no.nav.person.pdl.leesah.innflytting.InnflyttingTilNorge
import no.nav.person.pdl.leesah.navn.Navn
import no.nav.person.pdl.leesah.utflytting.UtflyttingFraNorge
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.record.TimestampType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockKExtension::class)
class LivshendelsemottakTest {
    @MockK
    lateinit var livshendelsebehandler: Livshendelsebehandler
    lateinit var livshendelsemottak: Livshendelsemottak

    @BeforeEach
    internal fun oppsett() {
        MockKAnnotations.init(this)
        clearAllMocks()
        livshendelsemottak = Livshendelsemottak((livshendelsebehandler))
        every { livshendelsebehandler.prosesserNyHendelse(any()) } returns Unit
    }

    @Test
    fun `skal avbryte prossesering av melding med opplysningstype som ikke støttes`() {
        // gitt
        val personhendelse = henteIkkeStøttetOpplysingstype()

        val cr =
            ConsumerRecord(
                "pdl.leesah-v1",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                personhendelse,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 0) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
    }

    @Test
    fun `skal håndtere dødsfall`() {
        // gitt
        val personhendelse = hentePersonhendelseForDødsfall()

        val cr =
            ConsumerRecord(
                "pdl.leesah-v1",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                personhendelse,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.DOEDSFALL_V1)
        assertThat(livshendelseSomSendesTilBehandling.captured.doedsdato).isEqualTo(personhendelse.doedsfall.doedsdato)
    }

    @Test
    fun `skal håndtere bostedsadresse`() {
        // gitt
        val personhendelse = hentePersonhendelseForBostedsadresse()

        val cr =
            ConsumerRecord(
                "pdl.leesah-v1",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                personhendelse,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.BOSTEDSADRESSE_V1)
        assertThat(livshendelseSomSendesTilBehandling.captured.flyttedato).isEqualTo(personhendelse.bostedsadresse.angittFlyttedato)
    }

    @Test
    fun `skal håndtere folkeregisteridentifikator`() {
        // gitt
        val personhendelse = hentePersonhendelseForFolkeregisteridentifikator()

        val cr =
            ConsumerRecord(
                "pdl.leesah-v1",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                personhendelse,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }

        assertThat(livshendelseSomSendesTilBehandling.captured).isNotNull
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.FOLKEREGISTERIDENTIFIKATOR_V1)
        assertThat(livshendelseSomSendesTilBehandling.captured.folkeregisteridentifikator?.identifikasjonsnummer).isEqualTo(
            personhendelse.folkeregisteridentifikator.identifikasjonsnummer,
        )
        assertThat(livshendelseSomSendesTilBehandling.captured.folkeregisteridentifikator?.type).isEqualTo(
            personhendelse.folkeregisteridentifikator.type,
        )
        assertThat(livshendelseSomSendesTilBehandling.captured.folkeregisteridentifikator?.status).isEqualTo(
            personhendelse.folkeregisteridentifikator.status,
        )
    }

    @Test
    fun `skal håndtere fødselsdato`() {
        // gitt
        val personhendelse = hentePersonhendelseForFødselsdato()

        val cr =
            ConsumerRecord(
                "pdl.leesah-v1",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                personhendelse,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.FOEDSELSDATO_V1)
        assertThat(
            livshendelseSomSendesTilBehandling.captured.foedselsdato?.foedselsdato,
        ).isEqualTo(personhendelse.foedselsdato.foedselsdato)
    }

    @Test
    fun `skal håndtere innflytting`() {
        // gitt
        val personhendelse = hentePersonhendelseForInnflytting()

        val cr =
            ConsumerRecord(
                "pdl.leesah-v1",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                personhendelse,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.INNFLYTTING_TIL_NORGE)
        assertThat(
            livshendelseSomSendesTilBehandling.captured.innflytting?.fraflyttingsland,
        ).isEqualTo(personhendelse.innflyttingTilNorge.fraflyttingsland)
    }

    @Test
    fun `skal håndtere navn`() {
        // gitt
        val personhendelse = hentePersonhendelseForNavn()

        val cr =
            ConsumerRecord(
                "pdl.leesah-v1",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                personhendelse,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.NAVN_V1)
        assertThat(livshendelseSomSendesTilBehandling.captured.navn?.fornavn).isEqualTo(personhendelse.navn.fornavn)
    }

    @Test
    fun `skal håndtere utflytting`() {
        // gitt
        val personhendelse = hentePersonhendelseForUtflytting()

        val cr =
            ConsumerRecord(
                "pdl.leesah-v1",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                personhendelse,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.UTFLYTTING_FRA_NORGE)
        assertThat(
            livshendelseSomSendesTilBehandling.captured.utflytting?.tilflyttingsland,
        ).isEqualTo(personhendelse.utflyttingFraNorge.tilflyttingsland)
        assertThat(
            livshendelseSomSendesTilBehandling.captured.utflytting?.utflyttingsdato,
        ).isEqualTo(personhendelse.utflyttingFraNorge.utflyttingsdato)
    }

    @Test
    fun `skal håndtere sivilstand`() {
        // gitt
        val personhendelse = hentePersonhendelseForSivilstand()

        val cr =
            ConsumerRecord(
                "pdl.leesah-v1",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                personhendelse,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.SIVILSTAND_V1)
        assertThat(
            livshendelseSomSendesTilBehandling.captured.sivilstand?.bekreftelsesdato,
        ).isEqualTo(personhendelse.sivilstand.bekreftelsesdato)
        assertThat(livshendelseSomSendesTilBehandling.captured.sivilstand?.sivilstand).isEqualTo(personhendelse.sivilstand.type)
    }

    @Test
    fun `skal håndtere korrigering av sivilstandendring`() {
        // gitt
        val personhendelse = hentePersonhendelseForSivilstandKorrigering()

        val cr =
            ConsumerRecord(
                "pdl.leesah-v1",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                personhendelse,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.SIVILSTAND_V1)
        assertThat(
            livshendelseSomSendesTilBehandling.captured.sivilstand?.bekreftelsesdato,
        ).isEqualTo(personhendelse.sivilstand.bekreftelsesdato)
        assertThat(livshendelseSomSendesTilBehandling.captured.sivilstand?.sivilstand).isEqualTo(personhendelse.sivilstand.type)
        assertThat(livshendelseSomSendesTilBehandling.captured.tidligereHendelseid).isNotEmpty()
        assertThat(livshendelseSomSendesTilBehandling.captured.tidligereHendelseid).isEqualTo(personhendelse.tidligereHendelseId)
    }

    @Test
    fun `skal håndtere manglende aktørid`() {
        // gitt
        val personidenterUtenAktørid =
            listOf(
                "12345678918",
                "12345678919",
                "12345678910",
                "23456789101235",
                "22345678910",
            )

        val personhendelse = henteMetadataTilPersonhendelse(personidenterUtenAktørid)
        val sivilstand =
            no.nav.person.pdl.leesah.sivilstand.Sivilstand
                .newBuilder()
                .setBekreftelsesdato(LocalDate.now())
                .setType("GIFT")
                .build()
        personhendelse.sivilstand = sivilstand
        personhendelse.opplysningstype = Opplysningstype.SIVILSTAND_V1.name

        val cr =
            ConsumerRecord(
                "pdl.leesah-v1",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                personhendelse,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        verify(exactly = 0) { livshendelsebehandler.prosesserNyHendelse(any()) }
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

        val personhendelse = henteMetadataTilPersonhendelse(langRekkePersonidenter)
        val sivilstand =
            no.nav.person.pdl.leesah.sivilstand.Sivilstand
                .newBuilder()
                .setBekreftelsesdato(LocalDate.now())
                .setType("GIFT")
                .build()
        personhendelse.sivilstand = sivilstand
        personhendelse.opplysningstype = Opplysningstype.SIVILSTAND_V1.name

        val cr =
            ConsumerRecord(
                "pdl.leesah-v1",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                personhendelse,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.SIVILSTAND_V1)
        assertThat(
            livshendelseSomSendesTilBehandling.captured.sivilstand?.bekreftelsesdato,
        ).isEqualTo(personhendelse.sivilstand.bekreftelsesdato)
        assertThat(livshendelseSomSendesTilBehandling.captured.sivilstand?.sivilstand).isEqualTo(personhendelse.sivilstand.type)
    }

    companion object {
        fun henteIkkeStøttetOpplysingstype(): Personhendelse {
            val personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.doedsfall = Doedsfall(LocalDate.now())
            personhendelse.opplysningstype = "TELEFONNUMMER_V1"

            return personhendelse
        }

        fun hentePersonhendelseForDødsfall(): Personhendelse {
            val personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.doedsfall = Doedsfall(LocalDate.now())
            personhendelse.opplysningstype = Opplysningstype.DOEDSFALL_V1.name

            return personhendelse
        }

        fun hentePersonhendelseForBostedsadresse(): Personhendelse {
            val personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.bostedsadresse =
                Bostedsadresse
                    .newBuilder()
                    .setAngittFlyttedato(LocalDate.now())
                    .setVegadresse(Vegadresse.newBuilder().setAdressenavn("Korketrekkeren 20").build())
                    .build()
            personhendelse.opplysningstype = Opplysningstype.BOSTEDSADRESSE_V1.name

            return personhendelse
        }

        fun hentePersonhendelseForFolkeregisteridentifikator(): Personhendelse {
            val personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.folkeregisteridentifikator =
                Folkeregisteridentifikator
                    .newBuilder()
                    .setIdentifikasjonsnummer("12345678910")
                    .setStatus("I_BRUK")
                    .setType("FNR")
                    .build()
            personhendelse.opplysningstype = Opplysningstype.FOLKEREGISTERIDENTIFIKATOR_V1.name

            return personhendelse
        }

        fun hentePersonhendelseForFødselsdato(): Personhendelse {
            val fødselsdato = Foedselsdato()
            fødselsdato.foedselsdato = LocalDate.now()
            val personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.foedselsdato = fødselsdato
            personhendelse.opplysningstype = Opplysningstype.FOEDSELSDATO_V1.name

            return personhendelse
        }

        fun hentePersonhendelseForInnflytting(): Personhendelse {
            val innflytting = InnflyttingTilNorge("POL", "Birk")
            val personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.innflyttingTilNorge = innflytting
            personhendelse.opplysningstype = Opplysningstype.INNFLYTTING_TIL_NORGE.name

            return personhendelse
        }

        fun hentePersonhendelseForNavn(): Personhendelse {
            val navn =
                Navn
                    .newBuilder()
                    .setFornavn("Stolpe")
                    .setEtternavn("Hekk")
                    .build()
            val personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.navn = navn
            personhendelse.opplysningstype = Opplysningstype.NAVN_V1.name
            return personhendelse
        }

        fun hentePersonhendelseForUtflytting(): Personhendelse {
            val utflytting =
                UtflyttingFraNorge
                    .newBuilder()
                    .setTilflyttingsland("POL")
                    .setUtflyttingsdato(LocalDate.now())
                    .build()
            val personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.utflyttingFraNorge = utflytting
            personhendelse.opplysningstype = Opplysningstype.UTFLYTTING_FRA_NORGE.name
            return personhendelse
        }

        fun hentePersonhendelseForSivilstand(): Personhendelse {
            val sivilstand =
                no.nav.person.pdl.leesah.sivilstand.Sivilstand
                    .newBuilder()
                    .setBekreftelsesdato(LocalDate.now())
                    .setType("GIFT")
                    .build()
            val personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.sivilstand = sivilstand
            personhendelse.opplysningstype = Opplysningstype.SIVILSTAND_V1.name
            return personhendelse
        }

        fun hentePersonhendelseForSivilstandKorrigering(): Personhendelse {
            val sivilstand =
                no.nav.person.pdl.leesah.sivilstand.Sivilstand
                    .newBuilder()
                    .setBekreftelsesdato(LocalDate.now())
                    .setType("GIFT")
                    .build()
            val personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.sivilstand = sivilstand
            personhendelse.tidligereHendelseId = "123"
            personhendelse.endringstype = Endringstype.KORRIGERT
            personhendelse.opplysningstype = Opplysningstype.SIVILSTAND_V1.name
            return personhendelse
        }

        internal fun henteMetadataTilPersonhendelse(): Personhendelse =
            henteMetadataTilPersonhendelse(listOf("2541031559331", "07486302423"))

        internal fun henteMetadataTilPersonhendelse(personidenter: List<String>): Personhendelse {
            val personhendelse =
                Personhendelse
                    .newBuilder()
                    .setHendelseId("567f35f1-b5c0-4457-8848-01d897d78bba")
                    .setPersonidenter(personidenter)
                    .setMaster("FREG")
                    .setOpprettet(Instant.now())
                    .setOpplysningstype("Ikke satt")
                    .setEndringstype(Endringstype.OPPRETTET)
                    .build()
            return personhendelse
        }
    }
}
