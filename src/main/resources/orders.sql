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
VALUES (1, 'John', 'Doe', 'john.doe@gmail.gov', '1', '1', 'milk', '2022-12-16');
INSERT INTO orders (order_id, first_name, last_name, email, cost, item_id, item_name, ship_date)
VALUES (2, 'Anna', 'Doe', 'anna.doe@gmail.gov', '1', '2', 'potato', '2022-12-15');
INSERT INTO orders (order_id, first_name, last_name, email, cost, item_id, item_name, ship_date)
VALUES (3, 'Brad', 'Doe', 'brad.doe@gmail.gov', '1', '3', 'beer', '2022-12-14');
INSERT INTO orders (order_id, first_name, last_name, email, cost, item_id, item_name, ship_date)
VALUES (4, 'Joe', 'Doe', 'joe.doe@gmail.gov', '4', '4', 'banana', '2022-12-13');
INSERT INTO orders (order_id, first_name, last_name, email, cost, item_id, item_name, ship_date)
VALUES (5, 'Mike', 'Doe', 'mike.doe@gmail.gov', '4', '4', 'banana', '2022-12-13');
INSERT INTO orders (order_id, first_name, last_name, email, cost, item_id, item_name, ship_date)
VALUES (6, 'Dany', 'Doe', 'dany.doe@gmail.gov', '5', '5', 'bread', '2022-12-12');
INSERT INTO orders (order_id, first_name, last_name, email, cost, item_id, item_name, ship_date)
VALUES (7, 'Tony', 'Doe', 'tony.doe@gmail.gov', '5', '5', 'bread', '2022-12-12');

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
