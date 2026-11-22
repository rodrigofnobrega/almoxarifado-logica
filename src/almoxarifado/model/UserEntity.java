package almoxarifado.model;

public class UserEntity {
    private final long id;
    private String name;
    private final String email;
    private String passwordHash;
    private boolean active;
    private Role role;

    public UserEntity(long id, String name, String email, String passwordHash, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
    }

    public long getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }

    public String getPasswordHash() { return passwordHash; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    public Role getRole() { return role; }

    public void setRole(Role role) { this.role = role; }

    public boolean isAdmin() { return role == Role.ADMIN; }

    @Override
    public String toString() {
        return "#" + id + " " + name + " <" + email + "> " + role + (active ? "" : " (inativo)");
    }
}
