CREATE TABLE orders
(
    order_id   BIGINT,
    first_name TEXT,
    last_name  TEXT,
    email      TEXT,
    cost       TEXT,
    item_id    TEXT,
    item_name  TEXT,
    ship_date  DATE
);

INSERT INTO orders (order_id, first_name, last_name, email, cost, item_id, item_name, ship_date)
VALUES (1, 'John', 'Doe', 'john.doe@gmail.com', '1', '1', 'milk', '2022-12-16');
INSERT INTO orders (order_id, first_name, last_name, email, cost, item_id, item_name, ship_date)
VALUES (2, 'Anna', 'Doe', 'anna.doe@gmail.com', '1', '2', 'potato', '2022-12-15');
INSERT INTO orders (order_id, first_name, last_name, email, cost, item_id, item_name, ship_date)
VALUES (3, 'Brad', 'Doe', 'brad.doe@gmail.com', '1', '3', 'beer', '2022-12-14');
INSERT INTO orders (order_id, first_name, last_name, email, cost, item_id, item_name, ship_date)
VALUES (4, 'Joe', 'Doe', 'joe.doe@gmail.gov', '1', '4', 'banana', '2022-12-13');

CREATE TABLE orders_output
(
    order_id   BIGINT,
    first_name TEXT,
    last_name  TEXT,
    email      TEXT,
    cost       TEXT,
    item_id    TEXT,
    item_name  TEXT,
    ship_date  DATE
);
