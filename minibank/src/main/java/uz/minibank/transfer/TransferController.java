package uz.minibank.transfer;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    // Ключ генерирует клиент — на намерение пользователя, а не на HTTP-запрос
    @PostMapping
    public void transfer(@RequestHeader("Idempotency-Key") UUID idempotencyKey,
                         @RequestBody TransferRequest request) {
        transferService.transfer(idempotencyKey, request.fromId(), request.toId(), request.amount());
    }
}
