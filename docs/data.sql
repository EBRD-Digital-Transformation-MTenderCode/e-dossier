CREATE KEYSPACE IF NOT EXISTS dossier
    WITH replication = {
    'class' : 'SimpleStrategy',
    'replication_factor' : 1
    };

CREATE TABLE IF NOT EXISTS dossier.tenders
(
    cp_id     text,
    owner     text,
    json_data text,
    primary key (cp_id)
);

CREATE TABLE IF NOT EXISTS  dossier.period (
    cpid text,
    ocid text,
    start_date timestamp,
    end_date timestamp,
    primary key(cpid, ocid)
);

CREATE TABLE IF NOT EXISTS  dossier.period_rules (
    country text,
    pmd text,
    value bigint,
    primary key(country, pmd)
);

INSERT INTO dossier.period_rules (country, pmd, value)  VALUES ('MD', 'GPA', 864000);