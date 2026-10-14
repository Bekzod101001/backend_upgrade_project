package uz.minibank.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import uz.minibank.kafka.KafkaTopics;
import uz.minibank.transfer.event.TransferCompletedEvent;

/**
 * Consumer знает о переводах ровно одно — контракт из transfer.event.
 * Ни одного импорта из самого uz.minibank.transfer: если завтра этот класс
 * поедет в отдельный сервис, меняется деплой, а не код.
 */
@Component
public class TransferNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(TransferNotificationListener.class);

    @KafkaListener(topics = KafkaTopics.TRANSFER_COMPLETED, groupId = "notification")
    public void onTransferCompleted(TransferCompletedEvent event) {
        // Пока просто лог. Отправка SMS и идемпотентность — следующий шаг.
        log.info("SMS: на счёт {} поступило {} тийин (transferId={}, eventId={})",
                event.toAccountId(), event.amount(), event.transferId(), event.eventId());
    }
}
