package no.nav.bidrag.person.hendelse.database

import jakarta.persistence.LockModeType
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.lang.Nullable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface HendelsemottakDao : JpaRepository<Hendelsemottak, Long> {
    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(hendelsemottak: Hendelsemottak): Hendelsemottak

    fun existsByHendelseidAndOpplysningstype(
        hendelseid: String,
        opplysningstype: Livshendelse.Opplysningstype,
    ): Boolean

    @Nullable
    fun findByHendelseidAndStatus(
        hendelseid: String,
        status: Status,
    ): Hendelsemottak?

    @Query(
        "select hm.id from Hendelsemottak hm " +
            "where hm.aktor.aktorid = :aktorid " +
            "and hm.status = no.nav.bidrag.person.hendelse.database.Status.OVERFØRT " +
            "and hm.endringstype not in (no.nav.bidrag.person.hendelse.domene.Endringstype.ANNULLERT)",
    )
    fun finnHendelsemottakIderMedStatusOverført(aktorid: String): Set<Long>

    @Query(
        "select hm.id from Hendelsemottak hm " +
            "where hm.status in " +
            "(no.nav.bidrag.person.hendelse.database.Status.MOTTATT, no.nav.bidrag.person.hendelse.database.Status.OVERFØRING_FEILET) " +
            "and hm.statustidspunkt < :statustidspunktFør",
    )
    fun idTilHendelserSomErKlarTilOverføring(statustidspunktFør: LocalDateTime): Set<Long>

    @Query(
        "from Hendelsemottak hm " +
            "where hm.status = no.nav.bidrag.person.hendelse.database.Status.OVERFØRT " +
            "and (hm.aktor.publisert is null or hm.aktor.publisert < :publisertFør)",
    )
    fun hentePubliseringsklareOverførteHendelser(publisertFør: LocalDateTime): Set<Hendelsemottak>

    @Query(
        "select hm.id from Hendelsemottak hm " +
            "where hm.status = :status " +
            "and hm.statustidspunkt < :statustidspunktFør",
    )
    fun henteIdTilHendelser(
        status: Status,
        statustidspunktFør: LocalDateTime,
    ): Set<Long>

    @Transactional
    fun deleteByIdIn(ider: Set<Long>): Long
}
