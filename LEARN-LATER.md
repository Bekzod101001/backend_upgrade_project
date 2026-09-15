# Learn Later

Темы, которые всплыли по ходу работы, но разбирать их прямо сейчас не время.
Когда тема разобрана — отмечаю `[x]` и оставляю ссылку на заметку или артефакт.

Формат:
- [ ] **Тема** — где всплыла / почему важно

---

## Hibernate / JPA

- [ ] **Entity lifecycle без `@Transactional` vs с `@Transactional`** — проверить в логах `org.hibernate.SQL` на `TransferService.transfer`.
  - Без `@Transactional`: каждый `findById` в своей транзакции → entity сразу DETACHED → `setBalance` Hibernate не отслеживает → `save()` = `em.merge()` → лишний SELECT + UPDATE + commit на каждый счёт.
    ```
    findById(from) → своя транзакция → commit → entity DETACHED
    findById(to)   → то же самое
    setBalance(...) → меняем detached объект
    save(from)     → merge → SELECT → UPDATE → commit
    save(to)       → merge → SELECT → UPDATE → commit
    ```
  - С `@Transactional`: entity MANAGED (есть снимок) → dirty checking при commit → 2 SELECT + 2 UPDATE, `save()` не нужен.
    ```
    begin
    findById(from) → SELECT → MANAGED
    findById(to)   → SELECT → MANAGED
    setBalance(...)
    commit → flush → dirty checking → UPDATE from, UPDATE to
    ```
  - Связано: `persist` vs `merge`, состояния entity (transient / managed / detached / removed), flush vs commit.

## Linux / процессы

- [ ] **SIGTERM** (и чем отличается от SIGKILL / `kill -9`) — всплыло при обсуждении, зачем `@Transactional` в `TransferService`: при rolling deploy под получает SIGTERM и запрос может оборваться посередине. Связано: graceful shutdown в Spring Boot.
- [ ] **OOM kill** — там же: процесс убивает ядро/Kubernetes при нехватке памяти, без шанса что-то «доделать».

## Infrastructure

- [ ] **Rolling deploy в Kubernetes** — как старые поды заменяются новыми и что происходит с запросами «в полёте».

## Databases / connections

- [ ] **Мёртвые соединения в connection pool** (HikariCP) — почему соединение из пула может оказаться разорванным и как пул это проверяет.

### Почему второй UPDATE может упасть при живой базе

Всплыло там же, в обсуждении `@Transactional` в `TransferService`.

- [ ] **Deadlock** — два перевода встречно блокируют A и B (A→B и B→A одновременно) и ждут друг друга; Postgres убивает одну из транзакций. Скоро понадобится в MiniBank.
- [ ] **Lock timeout** — транзакция слишком долго ждёт чужую блокировку строки и падает с ошибкой.
- [ ] **Optimistic locking / `@Version` / `OptimisticLockException`** — вместо блокировки проверяем при UPDATE, что строку никто не изменил с момента чтения. Скоро понадобится в MiniBank.
- [ ] **CHECK constraint** — правило на уровне таблицы (например, `balance >= 0`), которое Postgres не даст нарушить.
- [ ] **Триггеры в Postgres** — функция, которую база сама вызывает на INSERT/UPDATE/DELETE. Уже используется в `TransferServiceTest` для имитации отказа.
- [ ] **«Счёт заблокирован»** — это не блокировка в БД, а бизнес-статус (frozen account): операция запрещена правилами, и код бросает ошибку посреди перевода.
