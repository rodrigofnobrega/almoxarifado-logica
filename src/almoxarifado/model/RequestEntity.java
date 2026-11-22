package almoxarifado.model;

import java.time.LocalDateTime;

public class RequestEntity {
    private final long id;
    private RequestStatusEnum status;
    private final String description;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final int quantity;
    private String adminComment;
    private final UserEntity user;
    private final ItemEntity item;
    private UserEntity decidedBy;

    public RequestEntity(long id, RequestStatusEnum status, String description, int quantity,
                         UserEntity user, ItemEntity item) {
        this.id = id;
        this.status = status;
        this.description = description;
        this.quantity = quantity;
        this.user = user;
        this.item = item;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public long getId() { return id; }

    public RequestStatusEnum getStatus() { return status; }

    public void setStatus(RequestStatusEnum status) { this.status = status; }

    public String getDescription() { return description; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getQuantity() { return quantity; }

    public String getAdminComment() { return adminComment; }

    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }

    public UserEntity getUser() { return user; }

    public ItemEntity getItem() { return item; }

    public UserEntity getDecidedBy() { return decidedBy; }

    public void setDecidedBy(UserEntity decidedBy) { this.decidedBy = decidedBy; }

    @Override
    public String toString() {
        return "#" + id + " " + status + " " + quantity + "x " + item.getName()
                + " por " + user.getEmail() + " \"" + description + "\""
                + (adminComment != null ? " comentário: \"" + adminComment + "\"" : "")
                + (decidedBy != null ? " decidido por " + decidedBy.getEmail() : "");
    }
}
