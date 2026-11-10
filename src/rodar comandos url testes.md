# Scripts (igual package.json)

* este arquivo: `src\rodar comandos url testes.md`

```json
{
  "scripts": {
    "start":     ".\\mvnw.cmd spring-boot:run",
    "debug":     "F5 no VS Code / play no IntelliJ",
    "test":      ".\\mvnw.cmd test",
    "test:one":  ".\\mvnw.cmd test \"-Dtest=UserServiceTest\"",
    "test:cov":  ".\\mvnw.cmd test jacoco:report",
    "build":     ".\\mvnw.cmd package -DskipTests",
    "clean":     ".\\mvnw.cmd clean"
  }
}
```

* todos os comandos rodando em PowerShell com o JAVA_HOME setado (ver nota no final)

# Rodar projeto / debugger

rodar projeto - ir no debugger e dar play

- abre em http://localhost:8080 - swagger em http://localhost:8080/swagger-ui.html
- parar: Ctrl+C no terminal
- VS Code: F5 (config em `.vscode/launch.json`, mainClass `com.ars.collectionmanager.CollectionManagerApplication`)
- IntelliJ: play/debugger na classe `CollectionManagerApplication`

# Testes

geral (suíte inteira):

```
.\mvnw.cmd test
```

* o teste de contexto (`CollectionManagerApplicationTests`) sobe o Spring completo -> precisa MySQL rodando

arquivo só:

```
.\mvnw.cmd test "-Dtest=UserServiceTest"
.\mvnw.cmd test "-Dtest=UserServiceTest#nomeDoMetodo"
```

cobertura (gera HTML):

```
.\mvnw.cmd test jacoco:report
```

- relatorio: `target\site\jacoco\index.html` (abrir no navegador)
- unitários puros (sem MySQL): `.\mvnw.cmd test "-Dtest=UserServiceTest,CollectionServiceTest,CollectionItemServiceTest"`

# Notas

* JAVA_HOME OBRIGATORIO em toda sessao nova (Lombok estoura no jdk-27):

```
$env:JAVA_HOME="C:\Program Files\Java\jdk-22"
```

* build do jar: `.\mvnw.cmd package -DskipTests` -> `target\collectionmanager-0.0.1-SNAPSHOT.jar`
* limpar build velho (pastas antigas do package): `.\mvnw.cmd clean`
