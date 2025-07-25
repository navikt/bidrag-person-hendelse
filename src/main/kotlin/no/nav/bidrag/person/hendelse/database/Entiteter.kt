package no.nav.bidrag.person.hendelse.database

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import no.nav.bidrag.person.hendelse.domene.Endringstype
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import org.hibernate.annotations.ColumnTransformer
import java.time.LocalDateTime

@Entity
class Aktor(
    @Column(nullable = false)
    val aktorid: String,
    @Column
    var publisert: LocalDateTime? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "aktor", cascade = arrayOf(CascadeType.MERGE))
    val hendelsemottak: Set<Hendelsemottak> = HashSet(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Aktor) return false
        return aktorid == other.aktorid
    }

    override fun hashCode(): Int = aktorid.hashCode() * 31
}

@Entity
class Hendelsemottak(
    val hendelseid: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "opplysningstype", nullable = false, updatable = false)
    val opplysningstype: Livshendelse.Opplysningstype,
    @Enumerated(EnumType.STRING)
    val endringstype: Endringstype,
    @Column(name = "personidenter", nullable = false, columnDefinition = "TEXT")
    val personidenter: String,
    @ManyToOne(cascade = arrayOf(CascadeType.MERGE, CascadeType.PERSIST))
    var aktor: Aktor,
    @Column(name = "opprettet", nullable = false, updatable = false)
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val tidligereHendelseid: String? = null,
    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    val hendelse: String = "",
    val master: String = "",
    val offset_pdl: Long = 0L,
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    var status: Status = Status.MOTTATT,
    var statustidspunkt: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)

enum class Status {
    MOTTATT,
    KANSELLERT,
    OVERFØRT,
    OVERFØRING_FEILET,
    PUBLISERT,
}

val opplynsingstyperAdresse =
    listOf(
        Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
        Livshendelse.Opplysningstype.OPPHOLDSADRESSE_V1,
        Livshendelse.Opplysningstype.KONTAKTADRESSE_V1,
        Livshendelse.Opplysningstype.INNFLYTTING_TIL_NORGE,
        Livshendelse.Opplysningstype.UTFLYTTING_FRA_NORGE,
        Livshendelse.Opplysningstype.ADRESSEBESKYTTELSE_V1,
    )

fun Livshendelse.Opplysningstype.erAdresseendring() = opplynsingstyperAdresse.contains(this)
