package almoxarifado.model;

import java.time.LocalDateTime;

public class ItemEntity {
    private final long id;
    private String name;
    /** Código patrimonial; null quando o item não possui código. */
    private Long assetCode;
    private int quantity;
    private int minimumStockLevel;
    private String type;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean available;
    private final UserEntity createdBy;
    private RecordEntity lastRecord;

    public ItemEntity(long id, String name, Long assetCode, int quantity, int minimumStockLevel,
                      String type, UserEntity createdBy) {
        this.id = id;
        this.name = name;
        this.assetCode = assetCode;
        this.quantity = quantity;
        this.minimumStockLevel = minimumStockLevel;
        this.type = type;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.available = true;
    }

    public long getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public Long getAssetCode() { return assetCode; }

    public void setAssetCode(Long assetCode) { this.assetCode = assetCode; }

    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getMinimumStockLevel() { return minimumStockLevel; }

    public void setMinimumStockLevel(int minimumStockLevel) { this.minimumStockLevel = minimumStockLevel; }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isAvailable() { return available; }

    public void setAvailable(boolean available) { this.available = available; }

    public UserEntity getCreatedBy() { return createdBy; }

    public RecordEntity getLastRecord() { return lastRecord; }

    public void setLastRecord(RecordEntity lastRecord) { this.lastRecord = lastRecord; }

    @Override
    public String toString() {
        return "#" + id + " " + name
                + (assetCode != null ? " [patrimônio " + assetCode + "]" : "")
                + " qtd=" + quantity + " " + type
                + " min=" + minimumStockLevel
                + (available ? "" : " (indisponível)");
    }
}
