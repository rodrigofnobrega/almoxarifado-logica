package almoxarifado.cli;

import almoxarifado.model.ItemEntity;
import almoxarifado.model.RecordEntity;
import almoxarifado.model.RequestEntity;
import almoxarifado.model.RequestStatusEnum;
import almoxarifado.model.Role;
import almoxarifado.model.UserEntity;
import almoxarifado.service.ItemService;
import almoxarifado.service.OperationService;
import almoxarifado.service.RecordService;
import almoxarifado.service.RequestService;
import almoxarifado.service.UserService;

import java.io.FileOutputStream;
import java.io.FileDescriptor;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

/** Interface de linha de comando. Faz o papel dos controllers + Spring Security da API. */
public class Main {
    static final String ADMIN_EMAIL = "admin@gmail.com";
    static final String ADMIN_PASSWORD = "admin";

    private final Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
    private final UserService userService = new UserService();
    private final ItemService itemService = new ItemService();
    private final RecordService recordService = new RecordService();
    private final OperationService operationService = new OperationService(itemService, recordService);
    private final RequestService requestService = new RequestService(itemService, operationService);

    private UserEntity currentUser;

    public static void main(String[] args) {
        // Fixa UTF-8 independente do locale do sistema (senão acentos viram '?').
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        new Main().run();
    }

    private void run() {
        seedAdmin();
        System.out.println("=== Controle de Almoxarifado ===");
        System.out.println("Administrador padrão: " + ADMIN_EMAIL + " / " + ADMIN_PASSWORD);

        while (true) {
            String option = currentUser == null ? guestMenu() : userMenu();
            if (option == null) {
                return; // fim da entrada
            }
            try {
                if (currentUser == null) {
                    if (!handleGuest(option)) {
                        return;
                    }
                } else {
                    handleUser(option);
                }
            } catch (NumberFormatException e) {
                System.out.println("Erro: valor numérico inválido.");
            } catch (IllegalArgumentException e) {
                System.out.println("Erro: opção inválida.");
            } catch (RuntimeException e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }

    // Cria o administrador inicial pelas mesmas operações públicas usadas por qualquer usuário.
    private void seedAdmin() {
        userService.save("Administrador", ADMIN_EMAIL, ADMIN_PASSWORD);
        userService.updateRole(ADMIN_EMAIL, Role.ADMIN);
    }

    private String guestMenu() {
        System.out.println();
        System.out.println("1) Entrar  2) Cadastrar usuário  0) Sair");
        return prompt("> ");
    }

    private boolean handleGuest(String option) {
        switch (option) {
            case "1" -> {
                currentUser = userService.authenticate(prompt("E-mail: "), prompt("Senha: "));
                System.out.println("Bem-vindo(a), " + currentUser.getName() + "!");
            }
            case "2" -> {
                UserEntity user = userService.save(prompt("Nome: "), prompt("E-mail: "), prompt("Senha: "));
                System.out.println("Usuário #" + user.getId() + " criado.");
            }
            case "0" -> {
                return false;
            }
            default -> System.out.println("Opção inválida.");
        }
        return true;
    }

    private String userMenu() {
        System.out.println();
        System.out.println("[" + currentUser.getEmail() + "]");
        System.out.println(" 1) Listar itens          2) Ver item             3) Nova solicitação");
        System.out.println(" 4) Listar solicitações   5) Solicitações por status");
        System.out.println(" 6) Cancelar solicitação  7) Alterar nome         8) Alterar senha");
        if (currentUser.isAdmin()) {
            System.out.println("--- administrador ---");
            System.out.println("10) Cadastrar item (entrada)  11) Consumir item     12) Excluir item");
            System.out.println("13) Atualizar item            14) Estoque mínimo    15) Aceitar solicitação");
            System.out.println("16) Recusar solicitação       17) Listar registros  18) Registros de um item");
            System.out.println("19) Listar usuários           20) Ativar usuário    21) Desativar usuário");
            System.out.println("22) Alterar perfil            23) Resumo");
        }
        System.out.println(" 0) Sair da conta");
        return prompt("> ");
    }

    private void handleUser(String option) {
        if (!currentUser.isAdmin() && option.length() > 1) {
            System.out.println("Opção inválida.");
            return;
        }
        switch (option) {
            case "1" -> print(itemService.findAllItems(prompt("Buscar (vazio = todos): ")));
            case "2" -> {
                ItemEntity item = itemService.findById(readLong("Id do item: "));
                System.out.println(item);
                System.out.println("  criado por " + item.getCreatedBy().getEmail() + " em "
                        + item.getCreatedAt().withNano(0) + ", último registro: " + item.getLastRecord());
            }
            case "3" -> {
                RequestEntity request = requestService.create(currentUser, readLong("Id do item: "),
                        readInt("Quantidade: "), prompt("Descrição: "));
                System.out.println("Solicitação criada: " + request);
            }
            case "4" -> print(requestService.findAll(currentUser));
            case "5" -> print(requestService.findByStatus(currentUser,
                    RequestStatusEnum.valueOf(prompt("Status (PENDENTE/ACEITO/RECUSADO/CANCELADO): ").toUpperCase())));
            case "6" -> {
                requestService.cancel(readLong("Id da solicitação: "), currentUser);
                System.out.println("Solicitação cancelada.");
            }
            case "7" -> {
                userService.updateUserName(currentUser, prompt("Novo nome: "));
                System.out.println("Nome atualizado.");
            }
            case "8" -> {
                userService.updatePassword(prompt("Senha atual: "), prompt("Nova senha: "),
                        prompt("Confirme a nova senha: "), currentUser.getId());
                System.out.println("Senha atualizada.");
            }
            case "10" -> {
                RecordEntity record = operationService.toRegister(prompt("Nome: "), readInt("Quantidade: "),
                        readInt("Estoque mínimo: "), prompt("Tipo unitário (ex.: unidade, caixa): "),
                        readOptionalLong("Código patrimonial (vazio = sem código): "), currentUser);
                System.out.println("Registro: " + record);
            }
            case "11" -> {
                ItemEntity item = itemService.findById(readLong("Id do item: "));
                boolean alert = operationService.toConsume(currentUser, item.getId(), readInt("Quantidade: "));
                System.out.println("Registro: " + item.getLastRecord());
                printLowStockAlert(item, alert);
            }
            case "12" -> System.out.println("Registro: " + operationService.toDelete(readLong("Id do item: "),
                    currentUser));
            case "13" -> {
                long id = readLong("Id do item: ");
                System.out.println("(deixe vazio para não alterar)");
                ItemEntity item = itemService.updateItem(id, prompt("Nome: "), prompt("Tipo: "),
                        readOptionalLong("Código patrimonial: "), readOptionalInt("Estoque mínimo: "),
                        readOptionalInt("Quantidade: "));
                System.out.println("Atualizado: " + item);
            }
            case "14" -> {
                ItemEntity item = itemService.findById(readLong("Id do item: "));
                boolean alert = itemService.updateMinimumStockLevel(item.getId(), readInt("Estoque mínimo: "));
                System.out.println("Estoque mínimo atualizado.");
                printLowStockAlert(item, alert);
            }
            case "15" -> {
                RequestEntity request = requestService.findById(readLong("Id da solicitação: "), currentUser);
                boolean alert = requestService.accept(request.getId(), readOptional("Comentário: "), currentUser);
                System.out.println("Solicitação aceita.");
                printLowStockAlert(request.getItem(), alert);
            }
            case "16" -> {
                requestService.decline(readLong("Id da solicitação: "), readOptional("Comentário: "), currentUser);
                System.out.println("Solicitação recusada.");
            }
            case "17" -> print(recordService.findAll(prompt("Buscar por item (vazio = todos): ")));
            case "18" -> print(recordService.findByItem(readLong("Id do item: ")));
            case "19" -> print(userService.findAll(prompt("Buscar (vazio = todos): ")));
            case "20" -> {
                userService.activateUser(readLong("Id do usuário: "));
                System.out.println("Usuário ativado.");
            }
            case "21" -> {
                userService.disableUser(readLong("Id do usuário: "));
                System.out.println("Usuário desativado.");
            }
            case "22" -> {
                userService.updateRole(prompt("E-mail: "),
                        Role.valueOf(prompt("Perfil (ADMIN/USER): ").toUpperCase().replace("ROLE_", "")));
                System.out.println("Perfil atualizado.");
            }
            case "23" -> {
                System.out.println("Solicitações pendentes: " + requestService.countByStatus(RequestStatusEnum.PENDENTE));
                System.out.println("Solicitações aceitas:   " + requestService.countByStatus(RequestStatusEnum.ACEITO));
                System.out.println("Solicitações recusadas: " + requestService.countByStatus(RequestStatusEnum.RECUSADO));
                System.out.println("Itens cadastrados:      " + itemService.count());
                System.out.println("Unidades disponíveis:   " + itemService.sumAvailableItemsQuantity());
                System.out.println("Usuários:               " + userService.count());
            }
            case "0" -> {
                System.out.println("Até logo, " + currentUser.getName() + ".");
                currentUser = null;
            }
            default -> System.out.println("Opção inválida.");
        }
    }

    // --- leitura de entrada ---

    private String prompt(String label) {
        System.out.print(label);
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }

    private String readOptional(String label) {
        String value = prompt(label);
        return value == null || value.isEmpty() ? null : value;
    }

    private int readInt(String label) {
        return Integer.parseInt(prompt(label));
    }

    private long readLong(String label) {
        return Long.parseLong(prompt(label));
    }

    private Integer readOptionalInt(String label) {
        String value = readOptional(label);
        return value == null ? null : Integer.valueOf(value);
    }

    private Long readOptionalLong(String label) {
        String value = readOptional(label);
        return value == null ? null : Long.valueOf(value);
    }

    private static void printLowStockAlert(ItemEntity item, boolean alert) {
        if (alert) {
            System.out.println("*** AVISO: estoque baixo de " + item.getName() + ": " + item.getQuantity()
                    + " unidade(s), mínimo é " + item.getMinimumStockLevel() + " ***");
        }
    }

    private static void print(List<?> list) {
        if (list.isEmpty()) {
            System.out.println("(nenhum resultado)");
        }
        for (Object element : list) {
            System.out.println(element);
        }
    }
}
