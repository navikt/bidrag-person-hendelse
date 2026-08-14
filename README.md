# bidrag-person-hendelse

## ⚠️ DETTE REPOET ER FLYTTET

> **Dette repoet er flyttet til [navikt/bidrag-backend](https://github.com/navikt/bidrag-backend).**
> Bruk det nye repoet for videre utvikling — dette repoet oppdateres ikke lenger.


Mottaksapplikasjon for personhendelser. Lytter på ulike hendelser fra PDL (fødsler, dødsfall mm). Distribuerer disse videre til Bisys via MQ. 

## Monitorering

#### Elastic
Applikasjonen logger til Elastic, eksempel på loggsøk:
[Skedulert overføring - sjekke identifisert antall meldinger](https://logs.adeo.no/app/r/s/29As9)

#### Prometheus
Mottatte meldinger telles etter opplysning- og endringstype. Bare opplysningstyper definert i Livshendelse.Opplysningstype telles, øvrige meldinger ignoreres. 
Eksempel på opptelling i Prometheus:
[bostedsadresse](https://prometheus.prod-gcp.nav.cloud.nais.io/graph?g0.expr=bidrag_personhendelse_bostedsadresse_opprettet_total&g0.tab=1&g0.stacked=0&g0.show_exemplars=0&g0.range_input=1h)

## Lokal kjøring
Appen bygges med maven og kan kjøres fra DevLauncher-klassen. Sett `-Dspring.profiles.active=dev` under Edit Configurations -> VM Options. Lokalt må man kjøre serveren sammen med [navkafka-docker-compose][1].

Oppsummert oppstartsprosedyre for navkafka-docker-compose testmiljø med Colima Docker for Mac (bruk dedikert terminal-vindu/ tab):
```
 > colima start
 > docker-compose build
 > docker-compose up

```


Topicene vi lytter på må da opprettes via deres api med følgende data:
```
{
  "topics": [
    {
      "topicName": "aapen-person-pdl-leesah-v1",
      "members": [
        {"member":"srvc01", "role":"CONSUMER"}
      ],
      "numPartitions": 3
    },
    {
      "topicName": "aapen-dok-journalfoering-v1-q1",
      "members": [
        {"member":"srvc01", "role":"CONSUMER"}
      ],
      "numPartitions": 3
    },
    {
      "topicName": "aapen-person-pdl-aktor-v1",
      "members": [
        {"member":"srvc01", "role":"CONSUMER"}
      ],
      "numPartitions": 3
    },
  ]
}
```
Dette kan for eksempel gjøres med følgende kommandoer:
```
curl -X POST "http://igroup:itest@localhost:8840/api/v1/topics" -H "Accept: application/json" -H "Content-Type: application/json" --data "{"name": "aapen-person-pdl-leesah-v1", "members": [{ "member": "srvc01", "role": "CONSUMER" }], "numPartitions": 3 }" \
curl -X POST "http://igroup:itest@localhost:8840/api/v1/topics" -H "Accept: application/json" -H "Content-Type: application/json" --data "{"name": "aapen-dok-journalfoering-v1-q1", "members": [{ "member": "srvc01", "role": "CONSUMER" }], "numPartitions": 3 }" \
curl -X POST "http://igroup:itest@localhost:8840/api/v1/topics" -H "Accept: application/json" -H "Content-Type: application/json" --data "{"name": "aapen-person-pdl-aktor-v1", "members": [{ "member": "srvc01", "role": "CONSUMER" }], "numPartitions": 3 }"
```

Se README i navkafka-docker-compose for mer info om hvordan man kjører den og kaller apiet.

## Produksjonssetting
Appen blir produksjonssatt ved push til release

## Henvendelser
For NAV-interne kan henvendelser rettes til #bidrag på slack. Ellers kan henvendelser rettes via et issue her på github-repoet.

[1]: https://github.com/navikt/navkafka-docker-compose
