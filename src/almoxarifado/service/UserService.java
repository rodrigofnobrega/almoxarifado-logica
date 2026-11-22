package almoxarifado.service;

import almoxarifado.exception.EntityNotFoundException;
import almoxarifado.exception.OperationErrorException;
import almoxarifado.exception.PasswordInvalidException;
import almoxarifado.exception.UnauthorizedAccessException;
import almoxarifado.model.Role;
import almoxarifado.model.UserEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class UserService {
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@gmail\\.com$";
    private static final int MIN_PASSWORD_LENGTH = 5;

    private final List<UserEntity> users = new ArrayList<>();
    private long nextId = 1;
    /** Cadastra um usuário comum (já ativo). */
    public UserEntity save(String name, String email, String password) {
        if (name == null || name.isBlank()) {
            throw new OperationErrorException("Nome não pode ser vazio.");
        }
        if (email == null || !email.matches(EMAIL_REGEX)) {
            throw new OperationErrorException("Formato do e-mail inválido (use @gmail.com).");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new PasswordInvalidException("Senha deve ter ao menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        if (findByEmailOrNull(email) != null) {
            throw new OperationErrorException("Já existe um usuário com o e-mail " + email);
        }

        UserEntity user = new UserEntity(nextId++, name, email, hash(password), Role.USER);
        users.add(user);

        return user;
    }

    public UserEntity authenticate(String email, String password) {
        UserEntity user = findByEmailOrNull(email);
        if (user == null) {
            throw new EntityNotFoundException("Usuário não encontrado.");
        }
        if (!user.isActive()) {
            throw new UnauthorizedAccessException("Usuário inativo");
        }
        if (!user.getPasswordHash().equals(hash(password))) {
            throw new PasswordInvalidException("Credenciais inválidas");
        }
        return user;
    }

    public UserEntity findById(long id) {
        for (UserEntity user : users) {
            if (user.getId() == id) {
                return user;
            }
        }
        throw new EntityNotFoundException("Usuário não encontrado com id='" + id + "'");
    }

    public UserEntity findByEmail(String email) {
        UserEntity user = findByEmailOrNull(email);
        if (user == null) {
            throw new EntityNotFoundException("Usuário não encontrado com email='" + email + "'");
        }
        return user;
    }

    /** Busca por nome ou e-mail (todas as palavras da busca precisam aparecer). */
    public List<UserEntity> findAll(String searchQuery) {
        List<UserEntity> result = new ArrayList<>();
        for (UserEntity user : users) {
            String text = user.getName() + " " + user.getEmail();
            if (Search.matchesAllTokens(text, searchQuery)) {
                result.add(user);
            }
        }
        return result;
    }

    public List<UserEntity> findAllByRole(Role role) {
        List<UserEntity> result = new ArrayList<>();
        for (UserEntity user : users) {
            if (user.getRole() == role) {
                result.add(user);
            }
        }
        return result;
    }

    public void updatePassword(String currentPassword, String newPassword, String confirmPassword, long id) {
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new PasswordInvalidException("Nova senha não é igual a confirma senha");
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new PasswordInvalidException("Senha deve ter ao menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }

        UserEntity user = findById(id);

        if (!user.getPasswordHash().equals(hash(currentPassword))) {
            throw new PasswordInvalidException("Senha atual é inválida");
        }

        user.setPasswordHash(hash(newPassword));
    }

    public void disableUser(long id) {
        UserEntity user = findById(id);
        if (!user.isActive()) {
            throw new EntityNotFoundException("O usuário já está desativado.");
        }
        user.setActive(false);
    }

    public void activateUser(long id) {
        UserEntity user = findById(id);
        if (user.isActive()) {
            throw new EntityNotFoundException("O usuário já está ativo.");
        }
        user.setActive(true);
    }

    public void updateRole(String email, Role role) {
        findByEmail(email).setRole(role);
    }

    public void updateUserName(UserEntity user, String name) {
        if (name == null || name.isBlank()) {
            throw new OperationErrorException("Nome não pode ser vazio.");
        }
        user.setName(name);
    }

    public int count() {
        return users.size();
    }

    private UserEntity findByEmailOrNull(String email) {
        for (UserEntity user : users) {
            if (user.getEmail().equals(email)) {
                return user;
            }
        }
        return null;
    }

    private static String hash(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
