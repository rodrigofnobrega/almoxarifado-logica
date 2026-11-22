package almoxarifado.exception;

public class NotAvailableQuantityException extends RuntimeException {
    public NotAvailableQuantityException() {
        super("Não há quantidades disponíveis suficientes para este objeto.");
    }

    public NotAvailableQuantityException(String message) {
        super(message);
    }
}
