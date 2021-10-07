create table customer
(
    id             int primary key auto_increment,
    first_name     varchar(45),
    middle_initial varchar(1),
    lasts_name     varchar(45),
    address        varchar(45),
    city           varchar(45),
    state          varchar(2),
    zip            varchar(5)
);