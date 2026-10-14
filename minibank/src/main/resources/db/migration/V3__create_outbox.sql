CREATE TABLE outbox
(
    id             BIGSERIAL PRIMARY KEY,
    -- Топик и ключ партиции решает автор события, не воркер: воркер универсален,
    -- он просто перекладывает то, что тут лежит, в Kafka
    topic          VARCHAR(255) NOT NULL,
    partition_key  VARCHAR(255) NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- NULL = ещё не отправлено. Это аналог reserved_at/attempts в Laravel jobs,
    -- только тут проще: доставка одна попытка за тик, retry — это следующий тик @Scheduled
    processed_at   TIMESTAMPTZ
);

-- Воркер каждый тик спрашивает "что ещё не отправлено". Частичный индекс держит
-- эту выборку дешёвой даже когда обработанных строк накопятся миллионы.
CREATE INDEX idx_outbox_unprocessed ON outbox (created_at) WHERE processed_at IS NULL;
