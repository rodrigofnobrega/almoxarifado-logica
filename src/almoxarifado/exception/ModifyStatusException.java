package almoxarifado.exception;

public class ModifyStatusException extends RuntimeException {
    public ModifyStatusException() {
        super("Não é possível alterar o status da solicitação");
    }

    public ModifyStatusException(String message) {
        super(message);
    }
}
