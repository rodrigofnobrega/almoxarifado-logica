package almoxarifado.service;

import almoxarifado.model.ItemEntity;
import almoxarifado.model.RecordEntity;
import almoxarifado.model.RecordOperationEnum;
import almoxarifado.model.UserEntity;

/** Operações de estoque que sempre geram um registro e atualizam o último registro do item. */
public class OperationService {
    private final ItemService itemService;
    private final RecordService recordService;

    public OperationService(ItemService itemService, RecordService recordService) {
        this.itemService = itemService;
        this.recordService = recordService;
    }

    /** Registro gerado fica em item.getLastRecord(). @return true se houve aviso de estoque baixo. */
    public boolean toConsume(UserEntity user, long itemId, int quantity) {
        boolean lowStockAlert = itemService.deleteOrConsumeItem(itemId, quantity);

        ItemEntity item = itemService.findById(itemId);
        RecordEntity record = recordService.save(user, item, quantity, RecordOperationEnum.CONSUMO);
        itemService.setLastRecord(item, record);

        return lowStockAlert;
    }

    public RecordEntity toRegister(String name, int quantity, int minimumStockLevel, String type, Long assetCode,
                                   UserEntity user) {
        ItemEntity item = itemService.createItem(name, quantity, minimumStockLevel, type, assetCode, user);
        RecordEntity record = recordService.save(user, item, quantity, RecordOperationEnum.CADASTRO);
        itemService.setLastRecord(item, record);

        return record;
    }

    /** Exclusão lógica: zera a quantidade e torna o item indisponível, mantendo o histórico. */
    public RecordEntity toDelete(long itemId, UserEntity user) {
        ItemEntity item = itemService.findById(itemId);

        RecordEntity record = recordService.save(user, item, item.getQuantity(), RecordOperationEnum.EXCLUSAO);
        itemService.setLastRecord(item, record);

        item.setQuantity(0);
        item.setAvailable(false);

        return record;
    }
}
