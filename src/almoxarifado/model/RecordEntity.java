package almoxarifado.model;

import java.time.LocalDateTime;

public class RecordEntity {
    private final long id;
    private final UserEntity user;
    private final ItemEntity item;
    private final int quantity;
    private final RecordOperationEnum operation;
    private final LocalDateTime date;

    public RecordEntity(long id, UserEntity user, ItemEntity item, int quantity, RecordOperationEnum operation) {
        this.id = id;
        this.user = user;
        this.item = item;
        this.quantity = quantity;
        this.operation = operation;
        this.date = LocalDateTime.now();
    }

    public long getId() { return id; }

    public UserEntity getUser() { return user; }

    public ItemEntity getItem() { return item; }

    public int getQuantity() { return quantity; }

    public RecordOperationEnum getOperation() { return operation; }

    public LocalDateTime getDate() { return date; }

    @Override
    public String toString() {
        return "#" + id + " " + operation + " " + quantity + "x " + item.getName()
                + " por " + user.getEmail() + " em " + date.withNano(0);
    }
}
