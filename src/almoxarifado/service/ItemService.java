package almoxarifado.service;

import almoxarifado.exception.EntityNotFoundException;
import almoxarifado.exception.ItemNotActiveException;
import almoxarifado.exception.NotAvailableQuantityException;
import almoxarifado.exception.OperationErrorException;
import almoxarifado.model.ItemEntity;
import almoxarifado.model.RecordEntity;
import almoxarifado.model.UserEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemService {
    private final List<ItemEntity> items = new ArrayList<>();
    private long nextId = 1;

    public List<ItemEntity> findAllItems(String searchQuery) {
        List<ItemEntity> result = new ArrayList<>();
        for (ItemEntity item : items) {
            if (Search.matchesAllTokens(item.getName(), searchQuery)) {
                result.add(item);
            }
        }
        return result;
    }

    public ItemEntity findById(long id) {
        for (ItemEntity item : items) {
            if (item.getId() == id) {
                return item;
            }
        }
        throw new EntityNotFoundException("Item não encontrado com id=" + id);
    }

    public ItemEntity findByName(String name) {
        ItemEntity item = findByNameOrNull(name);
        if (item == null) {
            throw new EntityNotFoundException("Item não encontrado com nome=" + name);
        }
        return item;
    }

    public ItemEntity findByCode(long assetCode) {
        ItemEntity item = findByCodeOrNull(assetCode);
        if (item == null) {
            throw new EntityNotFoundException("Item não encontrado com código=" + assetCode);
        }
        return item;
    }

    /**
     * Cadastra um item. Se já existir item com o mesmo código patrimonial (ou, sem código, com o mesmo nome),
     * a quantidade é somada ao existente. Mesmo código com nome diferente é rejeitado.
     */
    public ItemEntity createItem(String name, int quantity, int minimumStockLevel, String type, Long assetCode,
                                 UserEntity createdBy) {
        if (name == null || name.isBlank() || type == null || type.isBlank()) {
            throw new OperationErrorException("Nome e tipo do item não podem ser vazios.");
        }
        if (quantity <= 0) {
            throw new OperationErrorException("Quantidade deve ser positiva.");
        }
        if (minimumStockLevel < 0 || (assetCode != null && assetCode < 0)) {
            throw new OperationErrorException("Estoque mínimo e código patrimonial não podem ser negativos.");
        }

        ItemEntity item = assetCode != null ? findByCodeOrNull(assetCode) : findByNameOrNull(name);

        if (item != null) {
            if (assetCode != null && !name.equalsIgnoreCase(item.getName())) {
                throw new OperationErrorException("Encontrado outro item com nome diferente e mesmo código patrimonial.");
            }
            increaseQuantity(item, quantity);
            item.setUpdatedAt(LocalDateTime.now());
        } else {
            item = new ItemEntity(nextId++, name, assetCode, quantity, minimumStockLevel, type, createdBy);
            items.add(item);
        }

        item.setAvailable(true);
        return item;
    }

    public void setLastRecord(ItemEntity item, RecordEntity record) {
        item.setLastRecord(record);
    }

    /** Campos null não são alterados. */
    public ItemEntity updateItem(long id, String name, String type, Long assetCode,
                                 Integer minimumStockLevel, Integer quantity) {
        ItemEntity item = findById(id);

        if ((assetCode != null && assetCode < 0) || (minimumStockLevel != null && minimumStockLevel < 0)
                || (quantity != null && quantity < 0)) {
            throw new OperationErrorException("Valores numéricos não podem ser negativos.");
        }
        if (assetCode != null) {
            ItemEntity other = findByCodeOrNull(assetCode);
            if (other != null && other != item) {
                throw new OperationErrorException("Já existe outro item com o código patrimonial " + assetCode);
            }
        }

        if (name != null && !name.isBlank()) {
            item.setName(name);
        }
        if (type != null && !type.isBlank()) {
            item.setType(type);
        }
        if (assetCode != null) {
            item.setAssetCode(assetCode);
        }
        if (minimumStockLevel != null) {
            item.setMinimumStockLevel(minimumStockLevel);
        }
        if (quantity != null) {
            item.setQuantity(quantity);
        }

        item.setUpdatedAt(LocalDateTime.now());
        return item;
    }

    /**
     * Consome quantity unidades do item; ao chegar a zero o item fica indisponível.
     *
     * @return true se deve ser emitido o aviso de estoque baixo: só quando o consumo faz a quantidade
     *         cair até o estoque mínimo. Se já estava no mínimo ou abaixo, o aviso não se repete.
     */
    public boolean deleteOrConsumeItem(long id, int quantity) {
        ItemEntity item = findById(id);

        if (!item.isAvailable()) {
            throw new ItemNotActiveException();
        }
        if (quantity > item.getQuantity()) {
            throw new NotAvailableQuantityException("Quantidade de itens disponiveis insuficientes.");
        }

        boolean wasAboveMinimum = item.getQuantity() > item.getMinimumStockLevel();

        decreaseQuantity(item, quantity);

        return wasAboveMinimum && isLowStock(item);
    }

    /** @return true se, com o novo mínimo, o item já está com estoque baixo (aviso de estoque baixo). */
    public boolean updateMinimumStockLevel(long id, int minimumStockLevel) {
        if (minimumStockLevel < 0) {
            throw new OperationErrorException("Estoque mínimo não pode ser negativo.");
        }
        ItemEntity item = findById(id);
        item.setMinimumStockLevel(minimumStockLevel);

        return isLowStock(item);
    }

    public boolean isLowStock(ItemEntity item) {
        return item.getQuantity() <= item.getMinimumStockLevel();
    }

    public int count() {
        return items.size();
    }

    public long sumAvailableItemsQuantity() {
        long total = 0;
        for (ItemEntity item : items) {
            if (item.isAvailable()) {
                total += item.getQuantity();
            }
        }
        return total;
    }

    private void increaseQuantity(ItemEntity item, int quantity) {
        if (quantity <= 0) {
            throw new OperationErrorException();
        }
        item.setQuantity(item.getQuantity() + quantity);
        item.setAvailable(true);
    }

    private void decreaseQuantity(ItemEntity item, int quantity) {
        if (quantity <= 0) {
            throw new OperationErrorException();
        }
        if (quantity > item.getQuantity()) {
            throw new OperationErrorException("Não há itens disponiveis suficientes para realizar a exclusão.");
        }
        item.setQuantity(item.getQuantity() - quantity);
        if (item.getQuantity() == 0) {
            item.setAvailable(false);
        }
    }

    private ItemEntity findByNameOrNull(String name) {
        for (ItemEntity item : items) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    private ItemEntity findByCodeOrNull(long assetCode) {
        for (ItemEntity item : items) {
            if (item.getAssetCode() != null && item.getAssetCode() == assetCode) {
                return item;
            }
        }
        return null;
    }
}
