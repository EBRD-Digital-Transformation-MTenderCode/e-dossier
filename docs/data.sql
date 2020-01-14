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