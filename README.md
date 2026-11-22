# Controle de Almoxarifado — CLI (Java puro)

Sistema genérico de controle de almoxarifado em linha de comando, sem Spring, Lombok, JPA ou banco,
feito para receber anotações JML (OpenJML). Os dados ficam em memória.

```
src/almoxarifado/model      entidades (ItemEntity, RecordEntity, RequestEntity, UserEntity) e enums
src/almoxarifado/service    regras de negócio (ItemService, OperationService, RequestService, ...)
src/almoxarifado/exception  exceções de negócio
src/almoxarifado/cli/Main   menu interativo (papel dos controllers + Spring Security)
test/SelfCheck              verificação das regras centrais com assert
```

## Compilar e executar

```
javac --release 17 -d out $(find src test -name '*.java')
java -cp out almoxarifado.cli.Main      # admin padrão: admin@gmail.com / admin
java -ea -cp out SelfCheck              # deve imprimir "SelfCheck OK"
```

## OpenJML

```
openjml -esc $(find src -name '*.java')   # verificação estática
openjml -rac -d out-rac $(find src -name '*.java') && java -cp out-rac:$OPENJML/jmlruntime.jar almoxarifado.cli.Main
```
