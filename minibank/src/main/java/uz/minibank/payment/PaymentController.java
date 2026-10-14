package uz.minibank.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uz.minibank.api.PaymentsApi;
import uz.minibank.api.model.ConfirmPaymentRequest;
import uz.minibank.api.model.PaymentInitRequest;
import uz.minibank.api.model.PaymentIntentResponse;

import java.util.UUID;

/**
 * Реализует интерфейс, сгенерированный из openapi/payments.yaml (Day 18, SDD/contract-first).
 * Путь, HTTP-метод и форма тела запроса/ответа заданы в спеке, не здесь —
 * несовместимое изменение спеки ломает компиляцию этого класса, а не всплывает на ревью постфактум.
 */
@RestController
public class PaymentController implements PaymentsApi {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public ResponseEntity<PaymentIntentResponse> initPayment(PaymentInitRequest paymentInitRequest) {
        return ResponseEntity.ok(paymentService.createIntent(paymentInitRequest));
    }

    @Override
    public ResponseEntity<PaymentIntentResponse> confirmPayment(UUID id, ConfirmPaymentRequest confirmPaymentRequest) {
        return ResponseEntity.ok(paymentService.confirmPayment(id, confirmPaymentRequest.getAmount()));
    }
}
