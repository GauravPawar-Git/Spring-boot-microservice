-- Phase 3: login credentials and a role for every user.
-- username is nullable: users created by an admin through POST /jpa/users have no login.

ALTER TABLE user_details ADD COLUMN username VARCHAR(30);
ALTER TABLE user_details ADD COLUMN password VARCHAR(100);
ALTER TABLE user_details ADD COLUMN role VARCHAR(20) DEFAULT 'USER' NOT NULL;
ALTER TABLE user_details ADD CONSTRAINT uk_user_details_username UNIQUE (username);

-- Demo accounts only: every seed user logs in with password "password123".
-- The column stores a BCrypt hash ($2a$ = algorithm, 10 = cost factor, then salt + hash), never the password itself.
UPDATE user_details
SET username = LOWER(name),
    password = '$2a$10$quAXPDahTyeSJDmVZXQbveur.atGTP8CaUm4gHc4PLns3yRsvClD6';

UPDATE user_details SET role = 'ADMIN' WHERE name = 'Rambo';
