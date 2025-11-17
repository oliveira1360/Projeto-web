
-- === USERS ===
INSERT INTO users (username, nick_name, email, password_hash, avatar_url, last_login, balance)
VALUES
    ('john_doe', 'JohnnyD', 'john@example.com', 'TesteBanana1', 'https://example.com/avatars/john.png', '2025-11-17 12:00:00', 1000),
    ('jane_smith', 'JaneS', 'jane@example.com', 'TesteBanana1', 'https://example.com/avatars/jane.png', '2025-11-17 11:45:00', 1500),
    ('mike_wilson', 'MikeW', 'mike@example.com', 'TesteBanana1', 'https://example.com/avatars/mike.png', '2025-11-17 13:15:00', 750),
    ('sarah_jones', 'SarahJ', 'sarah@example.com', 'TesteBanana1', 'https://example.com/avatars/sarah.png', '2025-11-17 10:30:00', 2000),
    ('alex_brown', 'AlexB', 'alex@example.com', 'TesteBanana1', 'https://example.com/avatars/alex.png', '2025-11-16 16:20:00', 500),
    ('emily_davis', 'EmilyD', 'emily@example.com', 'TesteBanana1', 'https://example.com/avatars/emily.png', '2025-11-17 08:30:00', 1200);

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
    ('token_abc123xyz789def456ghi', 1, 1731839400000, 1731845400000),
    ('token_jkl012mno345pqr678stu', 2, 1731843400000, 1731849400000),
    ('token_vwx901yza234bcd567efg', 3, 1731847400000, 1731853400000),
    ('token_hij890klm123nop456qrs', 4, 1731851400000, 1731857400000),
    ('token_tuv789wxy012zab345cde', 5, 1731855400000, 1731861400000),
    ('token_fgh678ijk901lmn234opq', 6, 1731859400000, 1731865400000);

-- === MATCHES ===
INSERT INTO matches (started_at, ended_at, winner_id, total_rounds, status)
VALUES
    ('2025-11-15 10:00:00', '2025-11-15 11:30:00', 1, 12, 'FINISHED'),
    ('2025-11-16 14:00:00', '2025-11-16 15:45:00', 3, 18, 'FINISHED'),
    ('2025-11-17 12:00:00', NULL, NULL, 12, 'ACTIVE'),
    ('2025-11-14 16:00:00', '2025-11-14 17:20:00', 2, 12, 'FINISHED'),
    ('2025-11-13 11:00:00', NULL, NULL, 18, 'CANCELLED'),
    ('2025-11-17 13:00:00', NULL, NULL, 18, 'ACTIVE');

-- === MATCH PLAYERS ===
INSERT INTO match_players (match_id, user_id, seat_number)
VALUES
-- Match 1 (3 players - finished)
(1, 1, 1),
(1, 2, 2),
(1, 3, 3),
-- Match 2 (3 players - finished)
(2, 2, 1),
(2, 3, 2),
(2, 4, 3),
-- Match 3 (4 players - ACTIVE - just started with round 1)
(3, 1, 1),
(3, 3, 2),
(3, 5, 3),
(3, 6, 4),
-- Match 4 (2 players - finished)
(4, 2, 1),
(4, 5, 2),
-- Match 5 (3 players - cancelled)
(5, 1, 1),
(5, 4, 2),
(5, 6, 3),
-- Match 6 (4 players - ACTIVE - just created, no rounds yet)
(6, 2, 1),
(6, 4, 2);

-- === ROUNDS ===
INSERT INTO rounds (match_id, round_number, winner_id)
VALUES
-- Match 1 rounds (finished)
(1, 1, 1),
(1, 2, 3),
(1, 3, 1),
(1, 4, 2),
-- Match 2 rounds (finished)
(2, 1, 3),
(2, 2, 3),
(2, 3, 2),
-- Match 3 rounds (ACTIVE - just started round 1)
(3, 1, NULL),
-- Match 4 rounds (finished)
(4, 1, 2),
(4, 2, 5),
-- Match 6 rounds (ACTIVE - just started round 1)
(6, 1, NULL);


-- === ROUND ORDER ===
INSERT INTO round_order (match_id, round_number, order_position, user_id)
VALUES
-- Match 3, Round 1
(3, 1, 1, 1),
(3, 1, 2, 3),
(3, 1, 3, 5),
(3, 1, 4, 6),
(6,1,1,2),
(6,1,2,4);

-- === TURN ===
-- Match 1, Round 1
INSERT INTO turn (match_id, round_number, user_id, hand, roll_number, score)
VALUES
    (1, 1, 1, '{"NINE","NINE","NINE","TEN","TEN"}', 2, 28),
    (1, 1, 2, '{"JACK","JACK","QUEEN","KING","ACE"}', 2, 14),
    (1, 1, 3, '{"TEN","TEN","TEN","QUEEN","KING"}', 2, 18),
-- Match 1, Round 2
    (1, 2, 1, '{"QUEEN","QUEEN","KING","KING","ACE"}', 3, 11),
    (1, 2, 2, '{"NINE","NINE","NINE","NINE","TEN"}', 3, 29),
    (1, 2, 3, '{"NINE","TEN","JACK","QUEEN","KING"}', 3, 20),
-- Match 1, Round 3
    (1, 3, 1, '{"TEN","TEN","JACK","JACK","QUEEN"}', 2, 22),
    (1, 3, 2, '{"QUEEN","QUEEN","KING","ACE","ACE"}', 3, 16),
    (1, 3, 3, '{"JACK","JACK","JACK","TEN","TEN"}', 2, 21),
-- Match 1, Round 4
    (1, 4, 1, '{"KING","KING","KING","QUEEN","ACE"}', 1, 19),
    (1, 4, 2, '{"NINE","NINE","TEN","TEN","JACK"}', 2, 25),
    (1, 4, 3, '{"ACE","ACE","ACE","KING","QUEEN"}', 3, 17),
-- Match 2, Round 1
    (2, 1, 2, '{"TEN","TEN","JACK","JACK","JACK"}', 3, 22),
    (2, 1, 3, '{"NINE","NINE","NINE","NINE","NINE"}', 3, 30),
    (2, 1, 4, '{"JACK","QUEEN","KING","ACE","ACE"}', 3, 11),
-- Match 2, Round 2
    (2, 2, 2, '{"JACK","JACK","JACK","QUEEN","QUEEN"}', 2, 20),
    (2, 2, 3, '{"NINE","NINE","TEN","TEN","JACK"}', 2, 26),
    (2, 2, 4, '{"KING","QUEEN","JACK","TEN","NINE"}', 3, 12),
-- Match 2, Round 3
    (2, 3, 2, '{"ACE","ACE","KING","KING","QUEEN"}', 2, 18),
    (2, 3, 3, '{"JACK","JACK","JACK","TEN","NINE"}', 3, 23),
    (2, 3, 4, '{"QUEEN","QUEEN","TEN","NINE","ACE"}', 2, 15),
-- Match 3, Round 1 (ACTIVE - just started)
    (3, 1, 1, '{"TEN","JACK","QUEEN","KING","ACE"}', 0, NULL),
    (3, 1, 3, '{"NINE","NINE","TEN","TEN","JACK"}', 0, NULL),
    (3, 1, 5, '{"QUEEN","QUEEN","QUEEN","KING","KING"}', 0, NULL),
    (3, 1, 6, '{"JACK","JACK","JACK","JACK","QUEEN"}', 0, NULL),
-- Match 4, Round 1
    (4, 1, 2, '{"TEN","TEN","JACK","JACK","JACK"}', 3, 22),
    (4, 1, 5, '{"NINE","NINE","NINE","TEN","JACK"}', 3, 27),
-- Match 4, Round 2
    (4, 2, 2, '{"JACK","JACK","QUEEN","KING","ACE"}', 2, 14),
    (4, 2, 5, '{"NINE","TEN","JACK","QUEEN","KING"}', 2, 18),
-- Match 6, Round 1 (ACTIVE - just started)
    (6, 1, 2, NULL, 0, NULL),
    (6, 1, 4, NULL, 0, NULL);

-- === LOBBIES ===
INSERT INTO lobbies (host_id, name, max_players, rounds, created_at)
VALUES
    (1, 'Friday Night Dice', 4, 20, '2025-11-17 18:00:00'),
    (3, 'Quick Game', 2, 10, '2025-11-17 19:00:00'),
    (4, 'Tournament Lobby', 6, 30, '2025-11-17 20:00:00'),
    (2, 'Casual Play', 3, 15, '2025-11-17 17:30:00');

-- === LOBBY PLAYERS ===
INSERT INTO lobby_players (lobby_id, user_id, joined_at)
VALUES
-- Lobby 1
(1, 1, '2025-11-17 18:00:00'),
(1, 2, '2025-11-17 18:02:00'),
(1, 5, '2025-11-17 18:05:00'),
-- Lobby 2
(2, 3, '2025-11-17 19:00:00'),
(2, 6, '2025-11-17 19:01:00'),
-- Lobby 3
(3, 4, '2025-11-17 20:00:00'),
(3, 1, '2025-11-17 20:03:00'),
(3, 3, '2025-11-17 20:05:00'),
-- Lobby 4
(4, 2, '2025-11-17 17:30:00'),
(4, 5, '2025-11-17 17:32:00');

-- === INVITES ===
INSERT INTO invites (invite_token, expires_at)
VALUES
    ('TEST-INVITE-ALPHA', '2099-12-31 23:59:59'),
    ('TEST-INVITE-BETA', '2099-12-31 23:59:59'),
    ('TEST-INVITE-GAMMA', '2099-12-31 23:59:59');