CREATE DATABASE IF NOT EXISTS transport_test;
USE transport_test;
CREATE TABLE IF NOT EXISTS bus_pass (
 pass_id VARCHAR(30) PRIMARY KEY,
 reg_no VARCHAR(30) NOT NULL,
 name VARCHAR(120) NOT NULL,
 route_no INT NOT NULL,
 amount DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
 INDEX ix_route(route_no)
);
CREATE TABLE IF NOT EXISTS users (
 id VARCHAR(60) PRIMARY KEY,
 pwd VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS wallets (
 wallet_id VARCHAR(30) PRIMARY KEY,
 balance DECIMAL(12,2) NOT NULL CHECK(balance >= 0)
);
