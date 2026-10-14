CREATE TABLE transfers
(
    id              BIGSERIAL PRIMARY KEY,
    -- Ключ приходит от клиента и опознаёт намерение пользователя, а не попытку отправки.
    -- UNIQUE — единственная настоящая защита от повторного проведения: проверка в коде не атомарна.
    idempotency_key UUID        NOT NULL,
    from_account_id BIGINT      NOT NULL REFERENCES accounts (id),
    to_account_id   BIGINT      NOT NULL REFERENCES accounts (id),
    -- В тийинах, как и balance
    amount          BIGINT      NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_transfers_idempotency_key UNIQUE (idempotency_key)
);
