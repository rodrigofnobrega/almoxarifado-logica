package almoxarifado.service;

import almoxarifado.exception.EntityNotFoundException;
import almoxarifado.model.ItemEntity;
import almoxarifado.model.RecordEntity;
import almoxarifado.model.RecordOperationEnum;
import almoxarifado.model.UserEntity;

import java.util.ArrayList;
import java.util.List;

public class RecordService {
    private final List<RecordEntity> records = new ArrayList<>();
    private long nextId = 1;

    public RecordEntity save(UserEntity user, ItemEntity item, int quantity, RecordOperationEnum operation) {
        RecordEntity record = new RecordEntity(nextId++, user, item, quantity, operation);
        records.add(record);
        return record;
    }

    /** Filtra pelo nome do item (todas as palavras da busca precisam aparecer). */
    public List<RecordEntity> findAll(String searchQuery) {
        List<RecordEntity> result = new ArrayList<>();
        for (RecordEntity record : records) {
            if (Search.matchesAllTokens(record.getItem().getName(), searchQuery)) {
                result.add(record);
            }
        }
        return result;
    }

    public RecordEntity findById(long id) {
        for (RecordEntity record : records) {
            if (record.getId() == id) {
                return record;
            }
        }
        throw new EntityNotFoundException("Registro não encontrado com o id='" + id + "'");
    }

    public List<RecordEntity> findByItem(long itemId) {
        List<RecordEntity> result = new ArrayList<>();
        for (RecordEntity record : records) {
            if (record.getItem().getId() == itemId) {
                result.add(record);
            }
        }
        return result;
    }

    public List<RecordEntity> findByUser(long userId) {
        List<RecordEntity> result = new ArrayList<>();
        for (RecordEntity record : records) {
            if (record.getUser().getId() == userId) {
                result.add(record);
            }
        }
        return result;
    }
}
