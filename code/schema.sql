CREATE DATABASE IF NOT EXISTS c3358_assg;
USE c3358_assg;

CREATE USER IF NOT EXISTS 'c3358'@'localhost' IDENTIFIED BY 'c3358PASS';
GRANT ALL PRIVILEGES ON c3358_assg.* TO 'c3358'@'localhost';
FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS users (
    username        VARCHAR(50)     NOT NULL PRIMARY KEY,
    password        VARCHAR(255)    NOT NULL,
    num_games       INT             NOT NULL DEFAULT 0,
    num_wins        INT             NOT NULL DEFAULT 0,
    avg_time_to_win FLOAT           NOT NULL DEFAULT 0.0,
    leaderboard_rank            INT             NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS online_users (
    username        VARCHAR(50)     NOT NULL PRIMARY KEY,
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);
