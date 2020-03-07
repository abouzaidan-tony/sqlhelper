SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";

CREATE TABLE `address` (
  `id` int(11) NOT NULL,
  `full_address` varchar(100) NOT NULL,
  `customer_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `author` (
  `id` int(11) NOT NULL,
  `name` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `author_book` (
  `author_id` int(11) NOT NULL,
  `book_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `book` (
  `id` int(11) NOT NULL,
  `name` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `customer` (
  `id` int(11) NOT NULL,
  `firstname` varchar(50) NOT NULL,
  `lastname` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `phone_number` (
  `id` int(11) NOT NULL,
  `phone_number` varchar(20) NOT NULL,
  `customer_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `address`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `author`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `book`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `customer`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `phone_number`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `address`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `author`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `book`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `customer`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `phone_number`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
COMMIT;
