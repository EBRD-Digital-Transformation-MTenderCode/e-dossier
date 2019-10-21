CREATE KEYSPACE IF NOT EXISTS procurer
    WITH replication = {
    'class' : 'SimpleStrategy',
    'replication_factor' : 1
    };

CREATE TABLE IF NOT EXISTS procurer.tenders
(
    cp_id     text,
    owner     text,
    json_data text,
    primary key (cp_id)
);