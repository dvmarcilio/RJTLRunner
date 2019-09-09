# Building and Running

First we need to locally install the rascal-rjtl JAR in our local maven repository.

```bash
mvn initialize
```

After that we can build the Runner´s JAR.

```bash
mvn clean compile assembly:single
```

This will create `target/RJTLRunner-x.x.x-SNAPSHOT-jar-with-dependencies`

With the JAR successfully created, we can pass several .java file paths to the runner to refactor for MutableMembersUsage and RemoveUnusedImports.

For instance:

```bash
java -jar .\RJTLRunner-0.0.2-SNAPSHOT-jar-with-dependencies.jar C:\dev\MutableMembersUsage\FuncaoDadosVersionavel.java C:\d
ev\MutableMembersUsage\FuncaoDados.java C:\dev\MutableMembersUsage\FuncaoTransacao.java
```

We have to make sure that the JAR is run with a JDK. This was achieved on windows by running:

```bash
C:\Progra~1\Java\jdk1.8.0_121\bin\java.exe -jar .\RJTLRunner-0.0.2-SNAPSHOT-jar-with-dependencies.jar C:\dev\MutableMembersUsage\FuncaoDadosVersionavel.java C:\d
ev\MutableMembersUsage\FuncaoDados.java C:\dev\MutableMembersUsage\FuncaoTransacao.java
```