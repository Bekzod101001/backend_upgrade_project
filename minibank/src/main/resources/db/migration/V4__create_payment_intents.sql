CREATE TABLE payment_intents
(
    id                      UUID PRIMARY KEY,
    account_id              BIGINT      NOT NULL REFERENCES accounts (id),
    provider_code           VARCHAR(64) NOT NULL,
    provider_account_number VARCHAR(64) NOT NULL,
    provider_display_name   VARCHAR(255),
    -- В тийинах, как и balance/amount у transfers. NULL до шага 2 (сумма ещё не введена).
    amount                  BIGINT,
    status                  VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at            TIMESTAMPTZ
);
