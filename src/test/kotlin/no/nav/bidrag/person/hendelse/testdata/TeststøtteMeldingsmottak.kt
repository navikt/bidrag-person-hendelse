package no.nav.bidrag.person.hendelse.testdata

import java.util.zip.CRC32
import no.nav.bidrag.person.hendelse.database.Aktor
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.Hendelsemottak
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.domene.Endringstype
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class TeststøtteMeldingsmottak(
    val databasetjeneste: Databasetjeneste,
) {
    fun henteAktør(aktørid: String): Aktor {
        val eksisteredeAktør = databasetjeneste.aktorDao.findByAktorid(aktørid)

        return if (eksisteredeAktør.isPresent) {
            eksisteredeAktør.get()
        } else {
            databasetjeneste.aktorDao.save(Aktor(aktørid))
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppretteOgLagreHendelsemottak(
        personidenter: List<String>,
        status: Status = Status.OVERFØRT,
    ): Hendelsemottak {
        val aktør = henteAktør(personidenter.first { it.length == 13 })

        val mottattHendelse =
            Hendelsemottak(
                CRC32().value.toString(),
                Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
                Endringstype.OPPRETTET,
                personidenter.joinToString { it },
                aktør,
            )

        mottattHendelse.status = status

        return databasetjeneste.hendelsemottakDao.save(mottattHendelse)
    }
}
