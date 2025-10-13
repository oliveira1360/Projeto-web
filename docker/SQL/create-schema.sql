-- DROP ALL TABLES (in reverse order of dependencies)
DROP TABLE IF EXISTS session_players CASCADE;
DROP TABLE IF EXISTS sessions CASCADE;
DROP TABLE IF EXISTS dice_rolls CASCADE;
DROP TABLE IF EXISTS hands CASCADE;
DROP TABLE IF EXISTS rounds CASCADE;
DROP TABLE IF EXISTS match_players CASCADE;
DROP TABLE IF EXISTS matches CASCADE;
DROP TABLE IF EXISTS player_hands_history CASCADE;
DROP TABLE IF EXISTS player_stats CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS Tokens CASCADE;

-- === USERS ===
CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(64),
                       nick_name VARCHAR(64) UNIQUE NOT NULL,
                       email VARCHAR(128) UNIQUE NOT NULL,
                       password_hash TEXT NOT NULL,
                       avatar_url TEXT,
                       creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       last_login TIMESTAMP,
                       balance DOUBLE PRECISION DEFAULT 0
);

-- === PLAYER STATS ===
CREATE TABLE player_stats (
                              user_id INT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                              total_games INT DEFAULT 0,
                              total_wins INT DEFAULT 0,
                              total_losses INT DEFAULT 0,
                              total_points INT DEFAULT 0,
                              longest_win_streak INT DEFAULT 0,
                              current_streak INT DEFAULT 0
);

create table Tokens
(
    token_validation VARCHAR(256) primary key,
    user_id          int references users (id),
    created_at       bigint not null,
    last_used_at     bigint not null
);

-- === PLAYER HANDS HISTORY ===
CREATE TABLE player_hands_history (
                                      user_id INT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                                      five_of_a_kind INT DEFAULT 0,
                                      four_of_a_kind INT DEFAULT 0,
                                      full_house INT DEFAULT 0,
                                      straight INT DEFAULT 0,
                                      three_of_a_kind INT DEFAULT 0,
                                      two_pair INT DEFAULT 0,
                                      one_pair INT DEFAULT 0
);

-- === MATCHES ===
CREATE TABLE matches (
                         id SERIAL PRIMARY KEY,
                         started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         ended_at TIMESTAMP,
                         winner_id INT REFERENCES users(id),
                         total_rounds INT CHECK (total_rounds BETWEEN 1 AND 60),
                         status VARCHAR(16) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FINISHED', 'CANCELLED'))
);

-- === MATCH PLAYERS ===
CREATE TABLE match_players (
                               match_id INT REFERENCES matches(id) ON DELETE CASCADE,
                               user_id INT REFERENCES users(id) ON DELETE CASCADE,
                               seat_number SMALLINT CHECK (seat_number BETWEEN 1 AND 6),
                               PRIMARY KEY (match_id, user_id)
);

-- === ROUNDS ===
CREATE TABLE rounds (
                        id SERIAL PRIMARY KEY,
                        match_id INT REFERENCES matches(id) ON DELETE CASCADE,
                        round_number INT NOT NULL CHECK (round_number BETWEEN 1 AND 60),
                        winner_id INT REFERENCES users(id),
                        UNIQUE (match_id, round_number, id)
);

-- === HANDS ===
CREATE TABLE hands (
                       id SERIAL PRIMARY KEY,
                       round_id INT REFERENCES rounds(id) ON DELETE CASCADE,
                       player_id INT REFERENCES users(id) ON DELETE CASCADE,
                       score INT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- === DICE ROLLS ===
CREATE TABLE dice_rolls (
                            id SERIAL PRIMARY KEY,
                            round_id INT REFERENCES rounds(id) ON DELETE CASCADE,
                            player_id INT REFERENCES users(id) ON DELETE CASCADE,
                            roll_number INT DEFAULT 1,
                            dice_values SMALLINT[5],
                            kept_dice BOOLEAN[5] DEFAULT ARRAY[false,false,false,false,false],
                            rolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- === LOBBIES ===
CREATE TABLE lobbies (
                         id SERIAL PRIMARY KEY,
                         host_id INT REFERENCES users(id) ON DELETE CASCADE,
                         name TEXT NOT NULL,
                         max_players INT NOT NULL CHECK (max_players > 0),
                         rounds INT NOT NULL CHECK (rounds > 0 AND mod(rounds, max_players) = 0),
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



-- === LOBBY PLAYERS ===
CREATE TABLE lobby_players (
                               lobby_id INT REFERENCES lobbies(id) ON DELETE CASCADE,
                               user_id INT REFERENCES users(id) ON DELETE CASCADE,
                               joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (lobby_id, user_id)
);