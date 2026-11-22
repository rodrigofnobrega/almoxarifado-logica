package almoxarifado.service;

import almoxarifado.exception.EntityNotFoundException;
import almoxarifado.exception.ModifyStatusException;
import almoxarifado.exception.OperationErrorException;
import almoxarifado.exception.UnauthorizedAccessException;
import almoxarifado.model.ItemEntity;
import almoxarifado.model.RequestEntity;
import almoxarifado.model.RequestStatusEnum;
import almoxarifado.model.UserEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RequestService {
    private final List<RequestEntity> requests = new ArrayList<>();
    private long nextId = 1;
    private final ItemService itemService;
    private final OperationService operationService;

    public RequestService(ItemService itemService, OperationService operationService) {
        this.itemService = itemService;
        this.operationService = operationService;
    }

    /** Toda solicitação nasce PENDENTE. */
    public RequestEntity create(UserEntity user, long itemId, int quantity, String description) {
        if (quantity <= 0) {
            throw new OperationErrorException("Quantidade deve ser positiva.");
        }
        if (description == null || description.isBlank()) {
            throw new OperationErrorException("Descrição não pode ser vazia.");
        }
        ItemEntity item = itemService.findById(itemId);

        RequestEntity request = new RequestEntity(nextId++, RequestStatusEnum.PENDENTE, description, quantity,
                user, item);
        requests.add(request);

        return request;
    }

    /**
     * Aceitar consome o item do estoque; se o consumo falhar, a solicitação continua PENDENTE.
     *
     * @return true se o consumo gerou aviso de estoque baixo.
     */
    public boolean accept(long id, String adminComment, UserEntity admin) {
        RequestEntity request = findById(id, admin);

        validateRequestStatus(request, RequestStatusEnum.ACEITO);

        boolean lowStockAlert = operationService.toConsume(request.getUser(), request.getItem().getId(),
                request.getQuantity());

        updateRequest(request, RequestStatusEnum.ACEITO, adminComment, admin);

        return lowStockAlert;
    }

    public void decline(long id, String adminComment, UserEntity admin) {
        RequestEntity request = findById(id, admin);

        updateRequest(request, RequestStatusEnum.RECUSADO, adminComment, admin);
    }

    public void cancel(long id, UserEntity user) {
        RequestEntity request = findById(id, user);

        updateRequest(request, RequestStatusEnum.CANCELADO, null, null);
    }

    /** Administrador vê todas; usuário comum vê apenas as suas. */
    public List<RequestEntity> findAll(UserEntity viewer) {
        List<RequestEntity> result = new ArrayList<>();
        for (RequestEntity request : requests) {
            if (canView(viewer, request)) {
                result.add(request);
            }
        }
        return result;
    }

    public List<RequestEntity> findByStatus(UserEntity viewer, RequestStatusEnum status) {
        List<RequestEntity> result = new ArrayList<>();
        for (RequestEntity request : requests) {
            if (request.getStatus() == status && canView(viewer, request)) {
                result.add(request);
            }
        }
        return result;
    }

    public List<RequestEntity> findByItem(UserEntity viewer, long itemId) {
        List<RequestEntity> result = new ArrayList<>();
        for (RequestEntity request : requests) {
            if (request.getItem().getId() == itemId && canView(viewer, request)) {
                result.add(request);
            }
        }
        return result;
    }

    public RequestEntity findById(long id, UserEntity viewer) {
        for (RequestEntity request : requests) {
            if (request.getId() == id) {
                if (!canView(viewer, request)) {
                    throw new UnauthorizedAccessException("O usuário='" + viewer.getEmail()
                            + "' está tentando obter a solicitação de outro usuário");
                }
                return request;
            }
        }
        throw new EntityNotFoundException("Solicitação não encontrado com id='" + id + "'");
    }

    public int countByStatus(RequestStatusEnum status) {
        int total = 0;
        for (RequestEntity request : requests) {
            if (request.getStatus() == status) {
                total++;
            }
        }
        return total;
    }

    private boolean canView(UserEntity viewer, RequestEntity request) {
        return viewer.isAdmin() || request.getUser().getId() == viewer.getId();
    }

    private void updateRequest(RequestEntity request, RequestStatusEnum status, String adminComment,
                               UserEntity decidedBy) {
        validateRequestStatus(request, status);

        request.setStatus(status);
        request.setAdminComment(adminComment);
        request.setUpdatedAt(LocalDateTime.now());
        request.setDecidedBy(decidedBy);
    }

    /** Só é possível mudar o status de uma solicitação PENDENTE. */
    private void validateRequestStatus(RequestEntity request, RequestStatusEnum status) {
        if (request.getStatus() == status) {
            throw new ModifyStatusException("Solicitação já foi " + status.toString().toLowerCase() + " anteriormente");
        }
        if (request.getStatus() != RequestStatusEnum.PENDENTE) {
            throw new ModifyStatusException("Não é possível alterar o status de uma solicitação que não está pendente");
        }
    }
}
