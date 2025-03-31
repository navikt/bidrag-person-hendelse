package no.nav.bidrag.person.hendelse.database

import jakarta.persistence.EntityManager
import no.nav.bidrag.person.hendelse.domene.Endringstype
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class Databasetjeneste(
    open val aktorDao: AktorDao,
    open val hendelsemottakDao: HendelsemottakDao,
    val egenskaper: Egenskaper,
    val entityManager: EntityManager,
) {
    fun oppdatereStatusPåHendelse(
        id: Long,
        nyStatus: Status,
    ) {
        val hendelse = hendelsemottakDao.findById(id)

        if (hendelse.isPresent) {
            val hendelsemottak = hendelse.get()
            hendelsemottak.status = nyStatus
            hendelsemottak.statustidspunkt = LocalDateTime.now()
            this.hendelsemottakDao.save(hendelsemottak)
        }
    }

    @Transactional
    fun oppdaterePubliseringstidspunkt(aktørid: String) {
        val aktør =
            aktorDao.findByAktorid(
                aktørid,
            )

        if (aktør.isPresent) {
            val statustidspunkt = LocalDateTime.now()
            entityManager.refresh(aktør.get())
            aktør.get().publisert = statustidspunkt
            aktør.get().hendelsemottak.filter { hm -> hm.status == Status.OVERFØRT }.forEach { hm ->
                hm.statustidspunkt = statustidspunkt
                hm.status = Status.PUBLISERT
            }
        } else {
            // Lagrer aktører som ikke er knyttet til PDL-hendelse (kan gjelde for kontoendringer)
            aktorDao.save(
                Aktor(
                    aktørid,
                ),
            )
        }
    }

    @Transactional
    fun oppdatereStatusPåHendelser(
        ider: List<Long>,
        nyStatus: Status,
    ) {
        for (id in ider) {
            oppdatereStatusPåHendelse(id, nyStatus)
        }
    }

    @Transactional(
        readOnly = false,
    )
    fun lagreHendelse(livshendelse: Livshendelse): Hendelsemottak {
        // Kansellere eventuell tidligere hendelse som er lagret i databasen med status mottatt
        var status = kansellereTidligereHendelse(livshendelse)

        // Sørge for at meldinger med endringstype KORRIGERT sendes videre
        if (Status.KANSELLERT == status && Endringstype.KORRIGERT == livshendelse.endringstype) {
            status = Status.MOTTATT
        }

        // Kansellerer hendelser om opphør av bostedsadresse. Endring av eksisterende bostedsadresse fører til utsending av to hendelser. Opprett for ny adresse og opphør for gammel.
        if (Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1 == livshendelse.opplysningstype &&
            Endringstype.OPPHOERT == livshendelse.endringstype
        ) {
            status = Status.KANSELLERT
        }

        val lagretAktør =
            aktorDao
                .findByAktorid(livshendelse.aktorid)
                .orElseGet {
                    aktorDao.save(Aktor(livshendelse.aktorid))
                }

        return hendelsemottakDao.save(
            Hendelsemottak(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
                livshendelse.endringstype,
                livshendelse.personidenter
                    .toSet()
                    .joinToString { it },
                lagretAktør,
                livshendelse.opprettet,
                livshendelse.tidligereHendelseid,
                Livshendelse.tilJson(
                    livshendelse,
                ),
                livshendelse.master,
                livshendelse.offset,
                status,
            ),
        )
    }

    @Transactional(
        propagation = Propagation.REQUIRED,
        readOnly = true,
        noRollbackFor = [Exception::class],
    )
    fun hentePubliseringsklareHendelser(): HashMap<Aktor, Pair<Set<String>, Hendelsemottak>> =
        tilHashMap(
            hendelsemottakDao.hentePubliseringsklareOverførteHendelser(
                LocalDateTime
                    .now()
                    .minusHours(
                        egenskaper.generelt.antallTimerSidenForrigePublisering.toLong(),
                    ),
                opplynsingstyperAdresse,
            ),
        )

    private fun tilHashMap(liste: Set<Hendelsemottak>): HashMap<Aktor, Pair<Set<String>, Hendelsemottak>> {
        val map =
            HashMap<Aktor, Pair<Set<String>, Hendelsemottak>>()
        liste.forEach {
            map.put(
                it.aktor,
                (
                    it.personidenter
                        .split(
                            ',',
                        ).map { ident -> ident.trim() }
                        .toSet() to it
                ),
            )
        }
        return map
    }

    private fun kansellereTidligereHendelse(livshendelse: Livshendelse): Status {
        val tidligereHendelseMedStatusMottatt =
            livshendelse.tidligereHendelseid?.let {
                hendelsemottakDao.findByHendelseidAndStatus(
                    it,
                    Status.MOTTATT,
                )
            }
        tidligereHendelseMedStatusMottatt?.status =
            Status.KANSELLERT
        tidligereHendelseMedStatusMottatt?.statustidspunkt =
            LocalDateTime.now()

        return if (Status.KANSELLERT == tidligereHendelseMedStatusMottatt?.status) {
            log.info(
                "Livshendelse med hendelseid ${tidligereHendelseMedStatusMottatt.hendelseid} " +
                    "ble erstattet av livshendelse med hendelseid ${livshendelse.hendelseid} og endringstype ${livshendelse.endringstype}.",
            )

            if (Endringstype.KORRIGERT != livshendelse.endringstype) {
                Status.KANSELLERT
            } else {
                Status.MOTTATT
            }
        } else {
            Status.MOTTATT
        }
    }

    companion object {
        val log: Logger =
            LoggerFactory.getLogger(
                Livshendelsebehandler::class.java,
            )
    }
}
