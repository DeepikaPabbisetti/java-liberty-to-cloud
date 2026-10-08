

CREATE SEQUENCE IF NOT EXISTS customer_seq   START WITH 1  INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS product_seq    START WITH 1  INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS inventory_seq  START WITH 1  INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS order_seq      START WITH 1  INCREMENT BY 10;
CREATE SEQUENCE IF NOT EXISTS order_item_seq START WITH 1  INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS customer_number_seq START WITH 1001 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS product_code_seq    START WITH 1001 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS order_number_seq    START WITH 1    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS customer (
    id               BIGINT         NOT NULL DEFAULT nextval('customer_seq'),
    customer_number  VARCHAR(20)    NOT NULL,
    first_name       VARCHAR(100)   NOT NULL,
    last_name        VARCHAR(100)   NOT NULL,
    email            VARCHAR(255)   NOT NULL,
    phone            VARCHAR(30),
    address          VARCHAR(255),
    city             VARCHAR(100),
    state            VARCHAR(100),
    country          VARCHAR(100),
    status           VARCHAR(20)    NOT NULL,
    created_at       TIMESTAMP      NOT NULL,
    updated_at       TIMESTAMP      NOT NULL,
    CONSTRAINT pk_customer            PRIMARY KEY (id),
    CONSTRAINT uq_customer_number     UNIQUE (customer_number),
    CONSTRAINT uq_customer_email      UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_customer_status ON customer (status);

CREATE TABLE IF NOT EXISTS product (
    id           BIGINT          NOT NULL DEFAULT nextval('product_seq'),
    product_code VARCHAR(20)     NOT NULL,
    name         VARCHAR(200)    NOT NULL,
    description  VARCHAR(1000),
    category     VARCHAR(100)    NOT NULL,
    price        NUMERIC(19, 4)  NOT NULL,
    status       VARCHAR(20)     NOT NULL,
    created_at   TIMESTAMP       NOT NULL,
    updated_at   TIMESTAMP       NOT NULL,
    CONSTRAINT pk_product        PRIMARY KEY (id),
    CONSTRAINT uq_product_code   UNIQUE (product_code),
    CONSTRAINT chk_product_price CHECK (price > 0)
);

CREATE INDEX IF NOT EXISTS idx_product_category ON product (category);
CREATE INDEX IF NOT EXISTS idx_product_status   ON product (status);

CREATE TABLE IF NOT EXISTS inventory (
    id                 BIGINT    NOT NULL DEFAULT nextval('inventory_seq'),
    product_id         BIGINT    NOT NULL,
    available_quantity INT       NOT NULL DEFAULT 0,
    reserved_quantity  INT       NOT NULL DEFAULT 0,
    reorder_level      INT       NOT NULL DEFAULT 0,
    updated_at         TIMESTAMP NOT NULL,
    version            BIGINT    NOT NULL DEFAULT 0,
    CONSTRAINT pk_inventory         PRIMARY KEY (id),
    CONSTRAINT uq_inventory_product UNIQUE (product_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT chk_available_qty    CHECK (available_quantity >= 0),
    CONSTRAINT chk_reserved_qty     CHECK (reserved_quantity  >= 0)
);

CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT         NOT NULL DEFAULT nextval('order_seq'),
    order_number VARCHAR(30)    NOT NULL,
    customer_id  BIGINT         NOT NULL,
    order_date   TIMESTAMP      NOT NULL,
    status       VARCHAR(20)    NOT NULL,
    total_amount NUMERIC(19, 4) NOT NULL,
    created_at   TIMESTAMP      NOT NULL,
    updated_at   TIMESTAMP      NOT NULL,
    version      BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_orders          PRIMARY KEY (id),
    CONSTRAINT uq_order_number    UNIQUE (order_number),
    CONSTRAINT fk_order_customer  FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT chk_order_total    CHECK (total_amount >= 0)
);

-- CREATE TABLE IF NOT EXISTS does not touch tables created by an earlier schema version
ALTER TABLE orders    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE inventory ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_order_customer ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_order_status   ON orders (status);
CREATE INDEX IF NOT EXISTS idx_order_date     ON orders (order_date);

CREATE TABLE IF NOT EXISTS order_item (
    id          BIGINT         NOT NULL DEFAULT nextval('order_item_seq'),
    order_id    BIGINT         NOT NULL,
    product_id  BIGINT         NOT NULL,
    quantity    INT            NOT NULL,
    unit_price  NUMERIC(19, 4) NOT NULL,
    line_total  NUMERIC(19, 4) NOT NULL,
    CONSTRAINT pk_order_item         PRIMARY KEY (id),
    CONSTRAINT fk_order_item_order   FOREIGN KEY (order_id)   REFERENCES orders  (id),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT chk_order_item_qty    CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_order_item_order   ON order_item (order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_product ON order_item (product_id);
