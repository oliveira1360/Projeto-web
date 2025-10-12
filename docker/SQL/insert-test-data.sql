-- === USERS ===
INSERT INTO users (username, nick_name, email, password_hash, avatar_url, balance)
VALUES
    ('alice123', 'Alice', 'alice@example.com', 'hash1', 'https://i.pravatar.cc/150?img=1', 100),
    ('bob456', 'Bob', 'bob@example.com', 'hash2', 'https://i.pravatar.cc/150?img=2', 150),
    ('charlie789', 'Charlie', 'charlie@example.com', 'hash3', 'https://i.pravatar.cc/150?img=3', 200),
    ('diana101', 'Diana', 'diana@example.com', 'hash4', 'https://i.pravatar.cc/150?img=4', 120);

-- === PLAYER STATS ===
INSERT INTO player_stats (user_id, total_games, total_wins, total_losses, total_points, longest_win_streak, current_streak)
VALUES
    (1, 10, 6, 4, 1500, 3, 1),
    (2, 8, 3, 5, 900, 2, 0),
    (3, 12, 7, 5, 1800, 4, 2),
    (4, 5, 2, 3, 700, 2, 1);

-- === PLAYER HANDS HISTORY ===
INSERT INTO player_hands_history (user_id, five_of_a_kind, four_of_a_kind, full_house, straight, three_of_a_kind, two_pair, one_pair)
VALUES
    (1, 1, 3, 2, 2, 5, 3, 10),
    (2, 0, 2, 1, 3, 4, 2, 8),
    (3, 2, 4, 3, 1, 6, 3, 12),
    (4, 0, 1, 2, 1, 2, 1, 5);

-- === MATCHES ===
INSERT INTO matches (started_at, ended_at, winner_id, total_rounds, status)
VALUES
    ('2025-10-06 10:00', '2025-10-06 10:30', 1, 5, 'FINISHED'),
    ('2025-10-06 11:00', NULL, NULL, 3, 'ACTIVE');

-- === MATCH PLAYERS ===
INSERT INTO match_players (match_id, user_id, seat_number)
VALUES
    (1, 1, 1),
    (1, 2, 2),
    (1, 3, 3),
    (2, 2, 1),
    (2, 4, 2);

-- === ROUNDS ===
INSERT INTO rounds (match_id, round_number, winner_id)
VALUES
    (1, 1, 2),
    (1, 2, 1),
    (1, 3, 3),
    (1, 4, 1),
    (1, 5, 1),
    (2, 1, NULL),
    (2, 2, NULL),
    (2, 3, NULL);

-- === HANDS ===
INSERT INTO hands (round_id, player_id, score)
VALUES
    (1, 1, 25),
    (1, 2, 30),
    (1, 3, 20),
    (2, 1, 40),
    (2, 2, 35),
    (2, 3, 25),
    (3, 1, 50),
    (3, 2, 45),
    (3, 3, 55),
    (4, 1, 60),
    (4, 2, 50),
    (4, 3, 45),
    (5, 1, 70),
    (5, 2, 60),
    (5, 3, 50);

-- === DICE ROLLS ===
INSERT INTO dice_rolls (round_id, player_id, roll_number, dice_values, kept_dice)
VALUES
    (1, 1, 1, ARRAY[1,2,3,4,5], ARRAY[false,false,false,false,false]),
    (1, 1, 2, ARRAY[1,2,3,5,5], ARRAY[true,true,true,false,true]),
    (1, 2, 1, ARRAY[2,2,4,5,6], ARRAY[true,true,false,false,false]),
    (1, 3, 1, ARRAY[1,1,2,3,4], ARRAY[true,true,false,false,false]);

-- === SESSIONS ===
INSERT INTO sessions (match_id, is_active, created_at)
VALUES
    (1, FALSE, '2025-10-06 09:50'),
    (2, TRUE, '2025-10-06 10:55');

-- === SESSION PLAYERS ===
INSERT INTO session_players (session_id, user_id, joined_at)
VALUES
    (1, 1, '2025-10-06 09:50'),
    (1, 2,
