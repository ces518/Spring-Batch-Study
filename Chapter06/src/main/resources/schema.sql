create table `Transactions`
(
    `id`                 int(32) primary key,
    `timestamp`          timestamp,
    `amount`             decimal(8, 2),
    `account_summary_id` int(32)
);

create table `Account_Summary`
(
    `id`              int(32) primary key,
    `account_number`   varchar(10),
    `current_balance` decimal(10, 2)
);