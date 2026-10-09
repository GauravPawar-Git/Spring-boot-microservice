-- Sample data (replaces the old data.sql). Ids are left to the database.

INSERT INTO user_details (name, birth_date) VALUES ('Rambo',   DATE '1985-07-06');
INSERT INTO user_details (name, birth_date) VALUES ('Shamlal', DATE '1992-11-23');
INSERT INTO user_details (name, birth_date) VALUES ('Vigo',    DATE '1998-02-14');
INSERT INTO user_details (name, birth_date) VALUES ('Diga',    DATE '2001-09-30');

INSERT INTO post (description, created_at, user_id)
SELECT 'Learning Spring Boot', TIMESTAMP '2026-10-01 09:00:00', id FROM user_details WHERE name = 'Rambo';
INSERT INTO post (description, created_at, user_id)
SELECT 'Constructor injection beats field injection', TIMESTAMP '2026-10-02 10:30:00', id FROM user_details WHERE name = 'Rambo';
INSERT INTO post (description, created_at, user_id)
SELECT 'Flyway keeps schemas in sync', TIMESTAMP '2026-10-03 14:15:00', id FROM user_details WHERE name = 'Shamlal';
INSERT INTO post (description, created_at, user_id)
SELECT 'What is the N+1 problem?', TIMESTAMP '2026-10-04 08:45:00', id FROM user_details WHERE name = 'Vigo';
INSERT INTO post (description, created_at, user_id)
SELECT 'Preparing for interviews', TIMESTAMP '2026-10-05 19:20:00', id FROM user_details WHERE name = 'Diga';
INSERT INTO post (description, created_at, user_id)
SELECT 'Docker is next', TIMESTAMP '2026-10-06 21:05:00', id FROM user_details WHERE name = 'Diga';
