/***

 table: hendelsemottak

 Prerequisite:
 Stop all bidrag-person-hendelse pods prior to deploy using the kubectl command:
  -> k scale deployment bidrag-person-hendelse --replicas 0

  This script will create a foreign key constraint for column aktor_id in hendelsemottak with reference to
  to the id field of table aktor.

  Population of the aktor_id column happens in a series of steps to optimize performance.

***/

insert into aktor(aktorid) select distinct substring(personidenter, '\d{13}') from  hendelsemottak;

create table mellomlagring(
    hendelsemottak_id integer not null,
    aktor_id integer,
    aktorid varchar(13)
);

insert into mellomlagring(hendelsemottak_id, aktorid) select id, substring(personidenter, '\d{13}') from  hendelsemottak;

create table hm as (
    select hendelseid, opplysningstype, endringstype, personidenter, tidligere_hendelseid, hendelse, master, offset_pdl, status, statustidspunkt, opprettet, h.id, aktor.id as aktor_id
    from hendelsemottak h
             inner join mellomlagring ml on ml.hendelsemottak_id = h.id
             inner join aktor on aktor.aktorid = ml.aktorid
);

drop table hendelsemottak;
alter table hm rename to hendelsemottak;

drop table mellomlagring;

alter table hendelsemottak
    alter column id
        type bigint,
    alter column id
        add generated by default as identity ( increment 1 start 5000000 minvalue 1 maxvalue 214748364700 cache 1 ),
    add constraint fk_aktor foreign key (aktor_id)
        references aktor (id) match simple
        on update no action
        on delete no action,
    add constraint hendelsemottak_pkey primary key (id);

create index index_hendelsemottak_aktor_id on hendelsemottak(aktor_id);
create index index_hendelsemottak_hendelseid on hendelsemottak(hendelseid);
create index index_hendelsemottak_status on hendelsemottak(status);
create index index_hendelsemottak_statustidspunkt on hendelsemottak(statustidspunkt)

/*** roll back ***

-- add indexes and constraints
alter table hendelsemottak add constraint hendelsemottak_pkey primary key (id);
create index if not exists index_hendelsemottak_hendelseid on hendelsemottak(hendelseid)
update hendelsemottak set aktor_id = null;
delete from aktor;

delete from flyway_schema_history where version = '2.0.4';

 */