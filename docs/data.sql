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

CREATE TABLE IF NOT EXISTS  dossier.history
(
    command_id text,
    command text,
    command_date timestamp,
    json_data text,
    primary key(command_id, command)
);

CREATE TABLE IF NOT EXISTS dossier.submission
(
    cpid text,
    ocid text,
    id uuid,
    status text,
    token_entity uuid,
    owner uuid,
    json_data text,
    primary key (cpid, ocid, id)
);

DROP TABLE IF EXISTS dossier.period_rules;

DROP TABLE IF EXISTS dossier.submission_quantity;

CREATE TABLE IF NOT EXISTS  dossier.rules (
    country text,
    pmd text,
    parameter text,
    value text,
    primary key(country, pmd, parameter)
);
/*
    Period duration
*/
INSERT INTO dossier.rules (country, pmd, parameter, value) VALUES ('MD', 'GPA', 'minSubmissionPeriodDuration', '864000');

/*
    Submissions minimum quantity
*/
INSERT INTO dossier.rules (country, pmd, parameter, value) VALUES ('MD', 'GPA', 'minQtySubmissionsForOpening', '3');

INSERT INTO dossier.rules (country, pmd, parameter, value) VALUES ('MD', 'GPA', 'extensionAfterUnsuspended', '604800');
INSERT INTO dossier.rules (country, pmd, parameter, value) VALUES ('MD', 'TEST_GPA', 'extensionAfterUnsuspended', '120');

DROP TABLE IF EXISTS dossier.rules;

CREATE TABLE IF NOT EXISTS dossier.rules (
    country text,
    pmd text,
    operationType test,
    parameter text,
    value text,
    primary key (country, pmd, operation_type, parameter)
);


INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'GPA', 'all', 'minSubmissionPeriodDuration', '864000');

INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'GPA', 'submissionPeriodEnd', 'minQtySubmissionsForReturning', '3');
INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'TEST_GPA', 'submissionPeriodEnd', 'minQtySubmissionsForReturning', '0');
INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'GPA', 'qualification', 'minQtySubmissionsForReturning', '0');
INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'TEST_GPA', 'qualification', 'minQtySubmissionsForReturning', '0');
INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'GPA', 'startSecondStage', 'minQtySubmissionsForReturning', '0');
INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'TEST_GPA', 'startSecondStage', 'minQtySubmissionsForReturning', '0');

INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'GPA', 'all', 'extensionAfterUnsuspended', '604800');
INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'TEST_GPA', 'all', 'extensionAfterUnsuspended', '120');

INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'GPA', 'submissionPeriodEnd', 'validStates', 'pending');
INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'TEST_GPA', 'submissionPeriodEnd', 'validStates', 'pending');
INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'GPA', 'qualification', 'validStates', 'pending');
INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'TEST_GPA', 'qualification', 'validStates', 'pending');
INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'GPA', 'startSecondStage', 'validStates', 'active');
INSERT INTO dossier.rules (country, pmd, operation_type, parameter, value) VALUES ('MD', 'TEST_GPA', 'startSecondStage', 'validStates', 'active');