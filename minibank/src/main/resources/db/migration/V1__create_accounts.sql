CREATE TABLE accounts
(
    id         BIGSERIAL PRIMARY KEY,
    owner_name VARCHAR(255) NOT NULL,
    -- Сумма в тийинах (минимальных единицах), чтобы избежать ошибок округления
    balance    BIGINT       NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO accounts (owner_name, balance)
VALUES ('Alice', 100000000), -- 1 000 000 UZS
       ('Bob', 50000000);    --   500 000 UZS
