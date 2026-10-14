package uz.minibank.payment;

/**
 * Внешняя система (поставщик услуги: коммуналка, мобильная связь и т.п.).
 * Вызов нарочно живёт за отдельным интерфейсом: реализация делает сетевой
 * запрос, который обязан остаться ВНЕ DB-транзакции (Day 17) — задержка
 * провайдера не должна держать коннекшн из пула.
 */
public interface ProviderClient {
    ProviderAccountInfo lookup(String providerCode, String providerAccountNumber);
}
