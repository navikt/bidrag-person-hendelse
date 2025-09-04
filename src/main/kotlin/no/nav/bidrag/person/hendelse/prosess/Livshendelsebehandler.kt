package no.nav.bidrag.person.hendelse.prosess

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.domene.Endringstype
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.domene.Livshendelse.Opplysningstype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class Livshendelsebehandler(
    val databasetjeneste: Databasetjeneste,
) {
    fun prosesserNyHendelse(livshendelse: Livshendelse) {
        when (livshendelse.opplysningstype) {
            Opplysningstype.ADRESSEBESKYTTELSE_V1 -> behandleAdressebeskyttelse(livshendelse)
            Opplysningstype.BOSTEDSADRESSE_V1 -> behandleAdresse(livshendelse, Opplysningstype.BOSTEDSADRESSE_V1)
            Opplysningstype.DOEDSFALL_V1 -> behandleDødsfall(livshendelse)
            Opplysningstype.FOEDSELSDATO_V1 -> behandleFødselsdato(livshendelse)
            Opplysningstype.FOLKEREGISTERIDENTIFIKATOR_V1 -> behandleFolkeregisteridentifikator(livshendelse)
            Opplysningstype.INNFLYTTING_TIL_NORGE -> behandleInnflytting(livshendelse)
            Opplysningstype.NAVN_V1 -> behandleNavn(livshendelse)
            Opplysningstype.UTFLYTTING_FRA_NORGE -> behandleUtflytting(livshendelse)
            Opplysningstype.SIVILSTAND_V1 -> behandleSivilstand(livshendelse)
            Opplysningstype.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1 -> behandleVerge(livshendelse)
            Opplysningstype.FOEDSEL_V1 -> log.error("Opplysningstype Foedsel ignoreres. Erstattet av Foedselsdato.")
            Opplysningstype.IKKE_STØTTET -> log.error("Forsøk på prosessere medling med opplysningstype som ikke støttes av løsningen.")
            Opplysningstype.KONTAKTADRESSE_V1 -> behandleAdresse(livshendelse, Opplysningstype.KONTAKTADRESSE_V1)
            Opplysningstype.OPPHOLDSADRESSE_V1 -> behandleAdresse(livshendelse, Opplysningstype.OPPHOLDSADRESSE_V1)
        }
    }

    private fun behandleAdressebeskyttelse(livshendelse: Livshendelse) {
        tellerAdressebeskyttelse.increment()

        if (databasetjeneste.hendelsemottakDao.existsByHendelseidAndOpplysningstype(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
            )
        ) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) " +
                    "med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne.",
            )
            return
        }

        sikkerLoggingAvLivshendelse(livshendelse, "Gradering: ${livshendelse.adressebeskyttelse}")

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerAdressebeskyttelseAnnullert.increment()
            Endringstype.KORRIGERT -> tellerAdressebeskyttelseKorrigert.increment()
            Endringstype.OPPHOERT -> tellerAdressebeskyttelseOpphørt.increment()
            Endringstype.OPPRETTET -> tellerAdressebeskyttelseOpprettet.increment()
        }

        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleVerge(livshendelse: Livshendelse) {
        tellerVerge.increment()

        if (databasetjeneste.hendelsemottakDao.existsByHendelseidAndOpplysningstype(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
            )
        ) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) " +
                    "med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne.",
            )
            return
        }

        sikkerLoggingAvLivshendelse(
            livshendelse,
            "Type: ${livshendelse.verge?.type}, omfang: ${livshendelse.verge?.vergeEllerFullmektig?.omfang}",
        )

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerVergeAnnullert.increment()
            Endringstype.KORRIGERT -> tellerVergeKorrigert.increment()
            Endringstype.OPPHOERT -> tellerVergeOpphørt.increment()
            Endringstype.OPPRETTET -> tellerVergeOpprettet.increment()
        }

        if (Endringstype.OPPHOERT != livshendelse.endringstype) {
            databasetjeneste.lagreHendelse(livshendelse)
        }
    }

    private fun behandleAdresse(
        livshendelse: Livshendelse,
        opplysningstype: Opplysningstype,
    ) {
        when (opplysningstype) {
            Opplysningstype.BOSTEDSADRESSE_V1 -> {
                tellerBostedsadresse.increment()
            }
            Opplysningstype.KONTAKTADRESSE_V1 -> tellerKontaktadresse.increment()
            Opplysningstype.OPPHOLDSADRESSE_V1 -> tellerOppholdsadresse.increment()
            else -> {
                return
            }
        }

        if (databasetjeneste.hendelsemottakDao.existsByHendelseidAndOpplysningstype(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
            )
        ) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) " +
                    "med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne.",
            )
            return
        }

        when (opplysningstype) {
            Opplysningstype.BOSTEDSADRESSE_V1 -> telleBostedsadresse(livshendelse.endringstype)
            Opplysningstype.KONTAKTADRESSE_V1 -> telleKontaktsadresse(livshendelse.endringstype)
            Opplysningstype.OPPHOLDSADRESSE_V1 -> telleOppholdsadresse(livshendelse.endringstype)
            else -> {
                return
            }
        }

        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun telleBostedsadresse(endringstype: Endringstype) {
        when (endringstype) {
            Endringstype.ANNULLERT -> tellerBostedsadresseAnnullert.increment()
            Endringstype.KORRIGERT -> tellerBostedsadresseKorrigert.increment()
            Endringstype.OPPHOERT -> tellerBostedsadresseOpphørt.increment()
            Endringstype.OPPRETTET -> tellerBostedsadresseOpprettet.increment()
        }
    }

    private fun telleKontaktsadresse(endringstype: Endringstype) {
        when (endringstype) {
            Endringstype.ANNULLERT -> tellerKontaktadresseAnnullert.increment()
            Endringstype.KORRIGERT -> tellerKontaktadresseKorrigert.increment()
            Endringstype.OPPHOERT -> tellerKontaktadresseOpphørt.increment()
            Endringstype.OPPRETTET -> tellerKontaktadresseOpprettet.increment()
        }
    }

    private fun telleOppholdsadresse(endringstype: Endringstype) {
        when (endringstype) {
            Endringstype.ANNULLERT -> tellerOppholdsadresseAnnullert.increment()
            Endringstype.KORRIGERT -> tellerOppholdsadresseKorrigert.increment()
            Endringstype.OPPHOERT -> tellerOppholdsadresseOpphørt.increment()
            Endringstype.OPPRETTET -> tellerOppholdsadresseOpprettet.increment()
        }
    }

    private fun behandleDødsfall(livshendelse: Livshendelse) {
        tellerDødsfall.increment()

        if (databasetjeneste.hendelsemottakDao.existsByHendelseidAndOpplysningstype(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
            )
        ) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) " +
                    "med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne.",
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerDødsfallAnnullert.increment()
            Endringstype.KORRIGERT -> tellerDødsfallKorrigert.increment()
            Endringstype.OPPHOERT -> tellerDødsfallOpphørt.increment()
            Endringstype.OPPRETTET -> tellerDødsfallOpprettet.increment()
        }

        sikkerLoggingAvLivshendelse(livshendelse, "dødsdato: ${livshendelse.doedsdato}")

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT -> {
                if (livshendelse.doedsdato == null) {
                    log.error("Mangler dødsdato. Ignorerer hendelse ${livshendelse.hendelseid}")
                    tellerDødsfallIgnorert.increment()
                } else {
                    databasetjeneste.lagreHendelse(livshendelse)
                }
            }

            Endringstype.ANNULLERT -> {
                if (livshendelse.tidligereHendelseid == null) {
                    log.warn("Mottatt annullert fødsel uten tidligereHendelseId, hendelseId ${livshendelse.hendelseid}")
                } else {
                    databasetjeneste.lagreHendelse(livshendelse)
                }
            }

            else -> {
                log.info(
                    "Ignorerer hendelse med id ${livshendelse.hendelseid} " +
                        "og opplysningstype ${livshendelse.opplysningstype}. Dødsdato: ${livshendelse.doedsdato}",
                )
            }
        }
    }

    private fun behandleFolkeregisteridentifikator(livshendelse: Livshendelse) {
        tellerFolkeregisteridentifikator.increment()

        if (databasetjeneste.hendelsemottakDao.existsByHendelseidAndOpplysningstype(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
            )
        ) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) " +
                    "med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne.",
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerFolkeregisteridentifikatorAnnullert.increment()
            Endringstype.KORRIGERT -> tellerFolkeregisteridentifikatorKorrigert.increment()
            Endringstype.OPPHOERT -> tellerFolkeregisteridentifikatorOpphørt.increment()
            Endringstype.OPPRETTET -> tellerFolkeregisteridentifikatorOpprettet.increment()
        }

        sikkerLoggingAvLivshendelse(livshendelse)

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT -> {
                if (livshendelse.folkeregisteridentifikator?.type == null) {
                    log.error("Mangler folkeregisteridentifikator.type. Ignorerer hendelse ${livshendelse.hendelseid}")
                    tellerFolkeregisteridentifikatorIgnorert.increment()
                }
            }

            else -> {
                log.warn("Hendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Endringstype: ${livshendelse.endringstype}")
            }
        }

        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleInnflytting(livshendelse: Livshendelse) {
        tellerInnflytting.increment()

        if (databasetjeneste.hendelsemottakDao.existsByHendelseidAndOpplysningstype(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
            )
        ) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) " +
                    "med opplysningstype ${livshendelse.hendelseid}. Ignorerer denne.",
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerInnflyttingAnnullert.increment()
            Endringstype.KORRIGERT -> tellerInnflyttingKorrigert.increment()
            Endringstype.OPPHOERT -> tellerInnflyttingOpphørt.increment()
            Endringstype.OPPRETTET -> tellerInnflyttingOpprettet.increment()
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT -> {
                sikkerLoggingAvLivshendelse(
                    livshendelse,
                    "Fraflyttingsland: ${livshendelse.innflytting?.fraflyttingsland}",
                )
                databasetjeneste.lagreHendelse(livshendelse)
            }

            Endringstype.ANNULLERT -> {
                sikkerLoggingAvLivshendelse(livshendelse)
                databasetjeneste.lagreHendelse(livshendelse)
            }

            else -> {
                tellerInnflyttingIgnorert.increment()
                sikkerLoggingAvLivshendelse(livshendelse, "Ikke av type OPPRETTET, KORRIGERT, eller ANNULLERT.")
            }
        }
    }

    private fun behandleNavn(livshendelse: Livshendelse) {
        tellerNavn.increment()

        if (databasetjeneste.hendelsemottakDao.existsByHendelseidAndOpplysningstype(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
            )
        ) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) " +
                    "med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne.",
            )
            return
        }

        sikkerLoggingAvLivshendelse(livshendelse)

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerNavnAnnullert.increment()
            Endringstype.KORRIGERT -> tellerNavnKorrigert.increment()
            Endringstype.OPPHOERT -> tellerNavnOpphørt.increment()
            Endringstype.OPPRETTET -> tellerNavnOpprettet.increment()
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT -> {
                val manglerFornavn = livshendelse.navn?.fornavn == null
                if (manglerFornavn || livshendelse.navn?.etternavn == null) {
                    val navnedel = if (manglerFornavn) "Fornavn" else "Etternavn"
                    log.warn("$navnedel mangler. Ignorerer navnehendelse med id ${livshendelse.hendelseid}")
                } else {
                    databasetjeneste.lagreHendelse(livshendelse)
                }
            }

            Endringstype.ANNULLERT -> {
                databasetjeneste.lagreHendelse(livshendelse)
            }

            else -> {
                log.info(
                    "Ignorerer navnehendelse med id ${livshendelse.hendelseid} " +
                        "av type ${livshendelse.opplysningstype}. Endringstype: ${livshendelse.endringstype}",
                )
            }
        }
    }

    private fun behandleFødselsdato(livshendelse: Livshendelse) {
        tellerFødselsdato.increment()

        if (databasetjeneste.hendelsemottakDao.existsByHendelseidAndOpplysningstype(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
            )
        ) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) " +
                    "med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne.",
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerFødselsdatoAnnulert.increment()
            Endringstype.KORRIGERT -> tellerFødselsdatoKorrigert.increment()
            Endringstype.OPPHOERT -> tellerFødselsdatoOpphørt.increment()
            Endringstype.OPPRETTET -> tellerFødselsdatoOpprettet.increment()
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT -> {
                sikkerLoggingAvLivshendelse(livshendelse, "fødselsdato: ${livshendelse.foedselsdato?.foedselsdato}")
                val fødselsdato = livshendelse.foedselsdato?.foedselsdato
                if (fødselsdato == null) {
                    tellerFødselsdatoIgnorert.increment()
                    log.warn("Mangler fødselsdato. Ignorerer hendelse ${livshendelse.hendelseid}")
                } else if (erUnder6mnd(fødselsdato)) {
                    tellerFødselsdatoIgnorert.increment()
                    // Kontrollen på fødeland fjernes siden det nå ligger i ny opplysningstype. Tanken er at Bisys vil håndtere utenlandske fødsler ok
                    databasetjeneste.lagreHendelse(livshendelse)
                }
            }

            Endringstype.ANNULLERT -> {
                sikkerLoggingAvLivshendelse(livshendelse)
                if (livshendelse.tidligereHendelseid == null) {
                    log.warn("Mottatt annullert fødselsdato uten tidligereHendelseId, hendelseId ${livshendelse.hendelseid}")
                } else {
                    databasetjeneste.lagreHendelse(livshendelse)
                }
            }

            else -> {
                log.info(
                    "Ignorerer livshendelse med id ${livshendelse.hendelseid} " +
                        "av type ${livshendelse.opplysningstype}. Endringstype: ${livshendelse.endringstype}",
                )
                sikkerLoggingAvLivshendelse(livshendelse)
            }
        }
    }

    private fun behandleUtflytting(livshendelse: Livshendelse) {
        tellerUtflytting.increment()

        if (databasetjeneste.hendelsemottakDao.existsByHendelseidAndOpplysningstype(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
            )
        ) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) " +
                    "med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne.",
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerUtflyttingAnnullert.increment()
            Endringstype.KORRIGERT -> tellerUtflyttingKorrigert.increment()
            Endringstype.OPPHOERT -> tellerUtflyttingOpphørt.increment()
            Endringstype.OPPRETTET -> tellerUtflyttingOpprettet.increment()
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT -> {
                sikkerLoggingAvLivshendelse(
                    livshendelse,
                    "utflyttingsdato: ${livshendelse.utflytting?.utflyttingsdato}",
                )
                databasetjeneste.lagreHendelse(livshendelse)
            }

            Endringstype.ANNULLERT -> {
                sikkerLoggingAvLivshendelse(livshendelse)
                databasetjeneste.lagreHendelse(livshendelse)
            }

            else -> {
                tellerUtflyttingIgnorert.increment()
                log.info(
                    "Ignorerer livshendelse med id ${livshendelse.hendelseid} " +
                        "av type ${livshendelse.opplysningstype}. Endringstype: ${livshendelse.endringstype}",
                )
                sikkerLoggingAvLivshendelse(livshendelse, "Ikke av type OPPRETTET eller ANNULLERT.")
            }
        }
    }

    private fun behandleSivilstand(livshendelse: Livshendelse) {
        tellerSivilstand.increment()

        if (databasetjeneste.hendelsemottakDao.existsByHendelseidAndOpplysningstype(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
            )
        ) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) " +
                    "med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne.",
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerSivilstandAnnullert.increment()
            Endringstype.KORRIGERT -> tellerSivilstandKorrigert.increment()
            Endringstype.OPPHOERT -> tellerSivilstandOpphørt.increment()
            Endringstype.OPPRETTET -> tellerSivilstandOpprettet.increment()
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT -> {
                sikkerLoggingAvLivshendelse(
                    livshendelse,
                    "sivilstandDato: ${livshendelse.sivilstand?.bekreftelsesdato}",
                )
                databasetjeneste.lagreHendelse(livshendelse)
            }

            Endringstype.ANNULLERT -> {
                sikkerLoggingAvLivshendelse(livshendelse)
                databasetjeneste.lagreHendelse(livshendelse)
            }

            else -> {
                log.info(
                    "Ignorerer livshendelse med id ${livshendelse.hendelseid} " +
                        "av type ${livshendelse.opplysningstype}. Endringstype: ${livshendelse.endringstype}",
                )
                sikkerLoggingAvLivshendelse(livshendelse, "Ikke av type OPPRETTET, KORRIGERT, eller ANNULLERT.")
            }
        }
    }

    private fun sikkerLoggingAvLivshendelse(
        livshendelse: Livshendelse,
        ekstraInfo: String = "",
    ) {
        slog.info(
            "Livshendelse mottatt: " +
                "hendelseId: ${livshendelse.hendelseid} " +
                "offset: ${livshendelse.offset}, " +
                "opplysningstype: ${livshendelse.opplysningstype}, " +
                "aktørid: ${livshendelse.hentGjeldendeAktørid()}, " +
                "endringstype: ${livshendelse.endringstype}, $ekstraInfo",
        )
    }

    private fun erUnder6mnd(fødselsDato: LocalDate): Boolean = LocalDate.now().isBefore(fødselsDato.plusMonths(6))

    private fun erUtenforNorge(fødeland: String?): Boolean =
        when (fødeland) {
            null, "NOR" -> false
            else -> true
        }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
        val slog: Logger = LoggerFactory.getLogger("secureLogger")

        val tellerLeesahDuplikat: Counter = Metrics.counter(tellernavn("leesah.duplikat"))

        const val ADRESSEBESKYTTELSE = "adressebeskyttelse"
        val tellerAdressebeskyttelse: Counter = Metrics.counter(tellernavn(ADRESSEBESKYTTELSE))
        val tellerAdressebeskyttelseAnnullert: Counter =
            Metrics.counter(tellernavn(ADRESSEBESKYTTELSE + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerAdressebeskyttelseKorrigert: Counter =
            Metrics.counter(tellernavn(ADRESSEBESKYTTELSE + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerAdressebeskyttelseOpphørt: Counter =
            Metrics.counter(tellernavn(ADRESSEBESKYTTELSE + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerAdressebeskyttelseOpprettet: Counter =
            Metrics.counter(tellernavn(ADRESSEBESKYTTELSE + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val BOSTEDSADRESSE = "bostedsadresse"
        val tellerBostedsadresse: Counter = Metrics.counter(tellernavn(BOSTEDSADRESSE))
        val tellerBostedsadresseAnnullert: Counter =
            Metrics.counter(tellernavn(BOSTEDSADRESSE + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerBostedsadresseKorrigert: Counter =
            Metrics.counter(tellernavn(BOSTEDSADRESSE + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerBostedsadresseOpphørt: Counter =
            Metrics.counter(tellernavn(BOSTEDSADRESSE + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerBostedsadresseOpprettet: Counter =
            Metrics.counter(tellernavn(BOSTEDSADRESSE + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val DØDSFALL = "doedsfall"
        val tellerDødsfall: Counter = Metrics.counter(tellernavn(DØDSFALL))
        val tellerDødsfallAnnullert: Counter =
            Metrics.counter(tellernavn(DØDSFALL + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerDødsfallKorrigert: Counter =
            Metrics.counter(tellernavn(DØDSFALL + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerDødsfallOpphørt: Counter =
            Metrics.counter(tellernavn(DØDSFALL + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerDødsfallOpprettet: Counter =
            Metrics.counter(tellernavn(DØDSFALL + ".${Endringstype.OPPRETTET.name.lowercase()}"))
        val tellerDødsfallIgnorert: Counter = Metrics.counter(tellernavn(DØDSFALL + ".ignorert"))

        const val FOLKEREGISTERIDENTIFIKATOR = "folkeregisteridentifikator"
        val tellerFolkeregisteridentifikator: Counter = Metrics.counter(tellernavn(FOLKEREGISTERIDENTIFIKATOR))
        val tellerFolkeregisteridentifikatorAnnullert: Counter =
            Metrics.counter(tellernavn(FOLKEREGISTERIDENTIFIKATOR + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerFolkeregisteridentifikatorKorrigert: Counter =
            Metrics.counter(tellernavn(FOLKEREGISTERIDENTIFIKATOR + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerFolkeregisteridentifikatorOpphørt: Counter =
            Metrics.counter(tellernavn(FOLKEREGISTERIDENTIFIKATOR + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerFolkeregisteridentifikatorOpprettet: Counter =
            Metrics.counter(tellernavn(FOLKEREGISTERIDENTIFIKATOR + ".${Endringstype.OPPRETTET.name.lowercase()}"))
        val tellerFolkeregisteridentifikatorIgnorert: Counter =
            Metrics.counter(tellernavn(FOLKEREGISTERIDENTIFIKATOR + ".ignorert"))

        const val FØDSEL = "foedsel"
        val tellerFødsel: Counter = Metrics.counter(tellernavn(FØDSEL))
        val tellerFødselIgnorert: Counter = Metrics.counter(tellernavn(FØDSEL + ".ignorert"))
        val tellerFødselAnnulert: Counter =
            Metrics.counter(tellernavn(FØDSEL + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerFødselKorrigert: Counter =
            Metrics.counter(tellernavn(FØDSEL + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerFødselOpphørt: Counter =
            Metrics.counter(tellernavn(FØDSEL + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerFødselOpprettet: Counter =
            Metrics.counter(tellernavn(FØDSEL + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val FØDSELSDATO = "foedselsdato"
        val tellerFødselsdato: Counter = Metrics.counter(tellernavn(FØDSELSDATO))
        val tellerFødselsdatoIgnorert: Counter = Metrics.counter(tellernavn(FØDSELSDATO + ".ignorert"))
        val tellerFødselsdatoAnnulert: Counter =
            Metrics.counter(tellernavn(FØDSELSDATO + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerFødselsdatoKorrigert: Counter =
            Metrics.counter(tellernavn(FØDSELSDATO + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerFødselsdatoOpphørt: Counter =
            Metrics.counter(tellernavn(FØDSELSDATO + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerFødselsdatoOpprettet: Counter =
            Metrics.counter(tellernavn(FØDSELSDATO + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val INNFLYTTING = "innflytting"
        val tellerInnflytting: Counter = Metrics.counter(tellernavn(INNFLYTTING))
        val tellerInnflyttingAnnullert: Counter =
            Metrics.counter(tellernavn(INNFLYTTING + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerInnflyttingKorrigert: Counter =
            Metrics.counter(tellernavn(INNFLYTTING + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerInnflyttingOpphørt: Counter =
            Metrics.counter(tellernavn(INNFLYTTING + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerInnflyttingOpprettet: Counter =
            Metrics.counter(tellernavn(INNFLYTTING + ".${Endringstype.OPPRETTET.name.lowercase()}"))
        val tellerInnflyttingIgnorert: Counter = Metrics.counter(tellernavn((INNFLYTTING + ".ignorert")))

        const val KONTAKTADRESSE = "kontaktadresse"
        val tellerKontaktadresse: Counter = Metrics.counter(tellernavn(KONTAKTADRESSE))
        val tellerKontaktadresseAnnullert: Counter =
            Metrics.counter(tellernavn(KONTAKTADRESSE + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerKontaktadresseKorrigert: Counter =
            Metrics.counter(tellernavn(KONTAKTADRESSE + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerKontaktadresseOpphørt: Counter =
            Metrics.counter(tellernavn(KONTAKTADRESSE + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerKontaktadresseOpprettet: Counter =
            Metrics.counter(tellernavn(KONTAKTADRESSE + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val NAVN = "navn"
        val tellerNavn: Counter = Metrics.counter(tellernavn(NAVN))
        val tellerNavnAnnullert: Counter =
            Metrics.counter(tellernavn(NAVN + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerNavnKorrigert: Counter =
            Metrics.counter(tellernavn(NAVN + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerNavnOpphørt: Counter =
            Metrics.counter(tellernavn(NAVN + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerNavnOpprettet: Counter =
            Metrics.counter(tellernavn(NAVN + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val OPPHOLDSADRESSE = "oppholdsadresse"
        val tellerOppholdsadresse: Counter = Metrics.counter(tellernavn(OPPHOLDSADRESSE))
        val tellerOppholdsadresseAnnullert: Counter =
            Metrics.counter(tellernavn(OPPHOLDSADRESSE + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerOppholdsadresseKorrigert: Counter =
            Metrics.counter(tellernavn(OPPHOLDSADRESSE + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerOppholdsadresseOpphørt: Counter =
            Metrics.counter(tellernavn(OPPHOLDSADRESSE + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerOppholdsadresseOpprettet: Counter =
            Metrics.counter(tellernavn(OPPHOLDSADRESSE + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val SIVILSTAND = "sivilstand"
        val tellerSivilstand: Counter = Metrics.counter(tellernavn(SIVILSTAND))
        val tellerSivilstandAnnullert: Counter =
            Metrics.counter(tellernavn(SIVILSTAND + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerSivilstandKorrigert: Counter =
            Metrics.counter(tellernavn(SIVILSTAND + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerSivilstandOpphørt: Counter =
            Metrics.counter(tellernavn(SIVILSTAND + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerSivilstandOpprettet: Counter =
            Metrics.counter(tellernavn(SIVILSTAND + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val UTFLYTTING = "utflytting"
        val tellerUtflytting: Counter = Metrics.counter(tellernavn(UTFLYTTING))
        val tellerUtflyttingAnnullert: Counter =
            Metrics.counter(tellernavn(UTFLYTTING + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerUtflyttingKorrigert: Counter =
            Metrics.counter(tellernavn(UTFLYTTING + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerUtflyttingOpphørt: Counter =
            Metrics.counter(tellernavn(UTFLYTTING + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerUtflyttingOpprettet: Counter =
            Metrics.counter(tellernavn(UTFLYTTING + ".${Endringstype.OPPRETTET.name.lowercase()}"))
        val tellerUtflyttingIgnorert: Counter = Metrics.counter(tellernavn(UTFLYTTING + ".ignorert"))

        const val VERGE = "verge"
        val tellerVerge: Counter = Metrics.counter(tellernavn(VERGE))
        val tellerVergeAnnullert: Counter =
            Metrics.counter(tellernavn(VERGE + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerVergeKorrigert: Counter =
            Metrics.counter(tellernavn(VERGE + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerVergeOpphørt: Counter =
            Metrics.counter(tellernavn(VERGE + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerVergeOpprettet: Counter =
            Metrics.counter(tellernavn(VERGE + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        private fun tellernavn(navn: String): String = "bidrag.personhendelse.$navn"
    }
}
