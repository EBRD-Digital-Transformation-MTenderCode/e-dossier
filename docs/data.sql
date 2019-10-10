CREATE KEYSPACE IF NOT EXISTS ocds WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};

CREATE TABLE IF NOT EXISTS  ocds.procurer_tender (
    cp_id text,
    owner text,
    json_data text,
    primary key(cp_id)
);