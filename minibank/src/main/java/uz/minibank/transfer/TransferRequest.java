package uz.minibank.transfer;

public record TransferRequest(Long fromId, Long toId, Long amount) {
}
