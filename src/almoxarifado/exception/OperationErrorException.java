package almoxarifado.exception;

public class OperationErrorException extends RuntimeException {
    public OperationErrorException() {
        super("Erro ao realizar operação");
    }

    public OperationErrorException(String message) {
        super(message);
    }
}
