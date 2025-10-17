
-- === USERS ===
INSERT INTO users (username, nick_name, email, password_hash, avatar_url, last_login, balance)
VALUES
    ('john_doe', 'JohnnyD', 'john@example.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'https://example.com/avatars/john.png', '2025-10-15 14:30:00', 1000),
    ('jane_smith', 'JaneS', 'jane@example.com', '$2a$10$zyxwvutsrqponmlkjihgfedcba', 'https://example.com/avatars/jane.png', '2025-10-16 09:15:00', 1500),
    ('mike_wilson', 'MikeW', 'mike@example.com', '$2a$10$1234567890abcdefghijklmnop', 'https://example.com/avatars/mike.png', '2025-10-16 18:45:00', 750),
    ('sarah_jones', 'SarahJ', 'sarah@example.com', '$2a$10$qwertyuiopasdfghjklzxcvbnm', 'https://example.com/avatars/sarah.png', '2025-10-17 10:00:00', 2000),
    ('alex_brown', 'AlexB', 'alex@example.com', '$2a$10$mnbvcxzlkjhgfdsapoiuytrewq', 'https://example.com/avatars/alex.png', '2025-10-14 16:20:00', 500),
    ('emily_davis', 'EmilyD', 'emily@example.com', '$2a$10$asdfghjklqwertyuiopzxcvbnm', 'https://example.com/avatars/emily.png', '2025-10-17 08:30:00', 1200);

-- === PLAYER STATS ===
INSERT INTO player_stats (user_id, total_games, total_wins, total_losses, total_points, longest_win_streak, current_streak)
VALUES
    (1, 45, 28, 17, 3450, 8, 3),
    (2, 38, 22, 16, 2890, 6, 0),
    (3, 52, 30, 22, 4120, 10, 5),
    (4, 29, 18, 11, 2340, 7, 2),
    (5, 41, 19, 22, 2650, 5, 0),
    (6, 33, 20, 13, 2980, 9, 4);

-- === TOKENS ===
INSERT INTO Tokens (token_validation, user_id, created_at, last_used_at)
VALUES
    ('token_abc123xyz789def456ghi', 1, 1697500000000, 1697586400000),
    ('token_jkl012mno345pqr678stu', 2, 1697510000000, 1697596800000),
    ('token_vwx901yza234bcd567efg', 3, 1697520000000, 1697606400000),
    ('token_hij890klm123nop456qrs', 4, 1697530000000, 1697616000000),
    ('token_tuv789wxy012zab345cde', 5, 1697540000000, 1697626400000),
    ('token_fgh678ijk901lmn234opq', 6, 1697550000000, 1697636800000);

-- === MATCHES ===
INSERT INTO matches (started_at, ended_at, winner_id, total_rounds, status)
VALUES
    ('2025-10-15 10:00:00', '2025-10-15 11:30:00', 1, 12, 'FINISHED'),
    ('2025-10-16 14:00:00', '2025-10-16 15:45:00', 3, 18, 'FINISHED'),
    ('2025-10-17 09:00:00', NULL, NULL, 24, 'ACTIVE'),
    ('2025-10-14 16:00:00', '2025-10-14 17:20:00', 2, 12, 'FINISHED'),
    ('2025-10-13 11:00:00', NULL, NULL, 18, 'CANCELLED');

-- === MATCH PLAYERS ===
INSERT INTO match_players (match_id, user_id, seat_number)
VALUES
-- Match 1 (3 players)
(1, 1, 1),
(1, 2, 2),
(1, 3, 3),
-- Match 2 (3 players)
(2, 2, 1),
(2, 3, 2),
(2, 4, 3),
-- Match 3 (4 players - active)
(3, 1, 1),
(3, 3, 2),
(3, 5, 3),
(3, 6, 4),
-- Match 4 (2 players)
(4, 2, 1),
(4, 5, 2),
-- Match 5 (3 players - cancelled)
(5, 1, 1),
(5, 4, 2),
(5, 6, 3);

-- === ROUNDS ===
INSERT INTO rounds (match_id, user_id, round_number, winner_id, roll_number)
VALUES
-- Match 1 rounds
(1, 1, 1, 1, 2),
(1, 2, 2, 3, 3),
(1, 3, 3, 1, 1),
(1, 1, 4, 2, 2),
-- Match 2 rounds
(2, 2, 1, 3, 3),
(2, 3, 2, 3, 2),
(2, 4, 3, 2, 1),
-- Match 3 rounds (active match)
(3, 1, 1, NULL, 2),
(3, 3, 2, NULL, 1),
-- Match 4 rounds
(4, 2, 1, 2, 3),
(4, 5, 2, 5, 2);

-- === TURN ===
-- Using DiceFace ENUM type for type-safe dice values
INSERT INTO turn (match_id, round_number, user_id, hand, roll_number, score)
VALUES
-- Match 1, Round 1
(1, 1, 1, '{"NINE","NINE","NINE","TEN","TEN"}', 2, 28),
(1, 1, 2, '{"JACK","JACK","QUEEN","KING","ACE"}', 2, 14),
(1, 1, 3, '{"TEN","TEN","TEN","QUEEN","KING"}', 2, 18),
-- Match 1, Round 2
(1, 2, 1, '{"QUEEN","QUEEN","KING","KING","ACE"}', 3, 11),
(1, 2, 2, '{"NINE","NINE","NINE","NINE","TEN"}', 3, 29),
(1, 2, 3, '{"NINE","TEN","JACK","QUEEN","KING"}', 3, 20),
-- Match 2, Round 1
(2, 1, 2, '{"TEN","TEN","JACK","JACK","JACK"}', 3, 22),
(2, 1, 3, '{"NINE","NINE","NINE","NINE","NINE"}', 3, 30),
(2, 1, 4, '{"JACK","QUEEN","KING","ACE","ACE"}', 3, 11),
-- Match 3, Round 1 (active)
(3, 1, 1, '{"TEN","JACK","QUEEN","KING","ACE"}', 2, 15),
(3, 1, 3, '{"NINE","NINE","TEN","TEN","JACK"}', 2, 26),
(3, 1, 5, '{"QUEEN","QUEEN","QUEEN","KING","KING"}', 2, 13),
(3, 1, 6, '{"JACK","JACK","JACK","JACK","QUEEN"}', 2, 19);


-- === LOBBIES ===
INSERT INTO lobbies (host_id, name, max_players, rounds, created_at)
VALUES
    (1, 'Friday Night Dice', 4, 20, '2025-10-17 18:00:00'),
    (3, 'Quick Game', 2, 10, '2025-10-17 19:00:00'),
    (4, 'Tournament Lobby', 6, 30, '2025-10-17 20:00:00'),
    (2, 'Casual Play', 3, 15, '2025-10-17 17:30:00');

-- === LOBBY PLAYERS ===
INSERT INTO lobby_players (lobby_id, user_id, joined_at)
VALUES
-- Lobby 1
(1, 1, '2025-10-17 18:00:00'),
(1, 2, '2025-10-17 18:02:00'),
(1, 5, '2025-10-17 18:05:00'),
-- Lobby 2
(2, 3, '2025-10-17 19:00:00'),
(2, 6, '2025-10-17 19:01:00'),
-- Lobby 3
(3, 4, '2025-10-17 20:00:00'),
(3, 1, '2025-10-17 20:03:00'),
(3, 3, '2025-10-17 20:05:00'),
-- Lobby 4
(4, 2, '2025-10-17 17:30:00'),
(4, 5, '2025-10-17 17:32:00');