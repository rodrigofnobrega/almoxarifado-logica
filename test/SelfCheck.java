import almoxarifado.exception.ItemNotActiveException;
import almoxarifado.exception.ModifyStatusException;
import almoxarifado.exception.NotAvailableQuantityException;
import almoxarifado.exception.OperationErrorException;
import almoxarifado.exception.UnauthorizedAccessException;
import almoxarifado.model.ItemEntity;
import almoxarifado.model.RecordOperationEnum;
import almoxarifado.model.RequestEntity;
import almoxarifado.model.RequestStatusEnum;
import almoxarifado.model.Role;
import almoxarifado.model.UserEntity;
import almoxarifado.service.ItemService;
import almoxarifado.service.OperationService;
import almoxarifado.service.RecordService;
import almoxarifado.service.RequestService;
import almoxarifado.service.UserService;

/** Verifica as regras de negócio centrais. Rodar com: java -ea -cp out SelfCheck */
public class SelfCheck {
    public static void main(String[] args) {
        UserService users = new UserService();
        ItemService items = new ItemService();
        RecordService records = new RecordService();
        OperationService ops = new OperationService(items, records);
        RequestService requests = new RequestService(items, ops);

        UserEntity admin = users.save("Admin", "admin@gmail.com", "admin");
        users.updateRole("admin@gmail.com", Role.ADMIN);
        assert users.authenticate("admin@gmail.com", "admin") == admin;

        UserEntity joao = users.save("João", "joao@gmail.com", "12345");

        // Cadastro: mesmo código patrimonial soma; mesmo código patrimonial com outro nome é rejeitado.
        ops.toRegister("Lápis", 10, 3, "unidade", 100L, admin);
        ItemEntity lapis = items.findByCode(100);
        ops.toRegister("lápis", 5, 3, "unidade", 100L, admin);
        assert lapis.getQuantity() == 15 && items.count() == 1;
        assertThrows(OperationErrorException.class, () -> ops.toRegister("Caneta", 1, 0, "unidade", 100L, admin));
        assert lapis.getLastRecord().getOperation() == RecordOperationEnum.CADASTRO;

        // Estoque mínimo: aviso só quando o consumo faz cruzar o mínimo, uma única vez.
        assert !ops.toConsume(admin, lapis.getId(), 11);   // 15 -> 4 (ainda acima de 3)
        assert ops.toConsume(admin, lapis.getId(), 1);     // 4 -> 3 (chegou ao mínimo: avisa)
        assert !ops.toConsume(admin, lapis.getId(), 1);    // 3 -> 2 (já estava no mínimo: não repete)
        assert lapis.getLastRecord().getOperation() == RecordOperationEnum.CONSUMO;
        assert items.updateMinimumStockLevel(lapis.getId(), 2);   // novo mínimo já atingido: avisa
        assert !items.updateMinimumStockLevel(lapis.getId(), 1);

        // Consumo: só com quantidade suficiente; ao zerar, o item fica indisponível.
        assertThrows(NotAvailableQuantityException.class, () -> ops.toConsume(admin, lapis.getId(), 3));
        ops.toConsume(admin, lapis.getId(), 2);            // 2 -> 0
        assert lapis.getQuantity() == 0 && !lapis.isAvailable();
        assertThrows(ItemNotActiveException.class, () -> ops.toConsume(admin, lapis.getId(), 1));

        // Solicitações: nascem PENDENTE, só saem de PENDENTE; aceitar consome do estoque.
        ops.toRegister("Papel A4", 20, 0, "resma", null, admin);
        ItemEntity papel = items.findByName("Papel A4");
        RequestEntity r1 = requests.create(joao, papel.getId(), 4, "aula");
        assert r1.getStatus() == RequestStatusEnum.PENDENTE;
        assert !requests.accept(r1.getId(), "ok", admin);   // 20 -> 16, mínimo 0
        assert r1.getStatus() == RequestStatusEnum.ACEITO && papel.getQuantity() == 16 && r1.getDecidedBy() == admin;
        assertThrows(ModifyStatusException.class, () -> requests.cancel(r1.getId(), joao));

        RequestEntity r2 = requests.create(joao, papel.getId(), 100, "muito");
        assertThrows(NotAvailableQuantityException.class, () -> requests.accept(r2.getId(), null, admin));
        assert r2.getStatus() == RequestStatusEnum.PENDENTE : "consumo falhou, continua pendente";
        requests.decline(r2.getId(), "sem estoque", admin);
        assert r2.getStatus() == RequestStatusEnum.RECUSADO;

        UserEntity maria = users.save("Maria", "maria@gmail.com", "12345");
        assertThrows(UnauthorizedAccessException.class, () -> requests.findById(r1.getId(), maria));
        assert requests.findAll(maria).isEmpty() && requests.findAll(admin).size() == 2;

        // Exclusão: zera e torna indisponível, mantendo o histórico.
        ops.toDelete(papel.getId(), admin);
        assert papel.getQuantity() == 0 && !papel.isAvailable();
        assert papel.getLastRecord().getOperation() == RecordOperationEnum.EXCLUSAO;
        assert records.findByItem(papel.getId()).size() == 3; // cadastro, consumo, exclusão

        System.out.println("SelfCheck OK");
    }

    private static void assertThrows(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            assert type.isInstance(e) : "esperava " + type.getSimpleName() + ", veio " + e;
            return;
        }
        throw new AssertionError("esperava " + type.getSimpleName());
    }
}
