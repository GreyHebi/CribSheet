# gRPC

1. [Подключаемые библиотеки](#libs)
   1. [Зависимости](#dependencys)
   2. [Плагины](#plugins)
2. [Полезные ссылки](#links)

## Подключаемые библиотеки<a id="libs"></a>

### <a id="dependencys">Зависимости</a>

Зависимости необходимые для работы с **gRPC**
```xml
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty</artifactId>
    <version>${java.grpc.version}</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-kotlin-stub</artifactId>
    <version>${grpc.kotlin.version}</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>${java.grpc.version}</version>
</dependency>
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-kotlin</artifactId>
    <version>${protobuf.version}</version>
</dependency>
```

Подключение спрингового стартера клиента **gRPC**
```xml
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-client-spring-boot-starter</artifactId>
    <version>2.13.1.RELEASE</version>
</dependency>
```

Для работы с потоковыми данными в котлине необходимо добавить работу с корутинами
```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-core</artifactId>
</dependency>
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-reactor</artifactId>
</dependency>
```

[К началу](#grpc)

### Плагины<a id="plugins"></a>

Обязательный(**!**) плагин для запуска кодогенератора
```xml
<plugin>
    <groupId>kr.motd.maven</groupId>
    <artifactId>os-maven-plugin</artifactId>
    <version>1.7.1</version> 
    <executions>
        <execution>
            <phase>initialize</phase>
            <goals>
                <goal>detect</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Для генерации сервисов и дто необходимо добавить плагин
```xml
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>0.6.1</version>
</plugin>
```
В конфигах можно добавить необходимые настройки
```xml
<configuration>
    <!-- Папка, в которой находятся файлы-спецификации *.proto.
    По-умолчанию, "${basedir}/src/main/proto" -->
    <protoSourceRoot>${basedir}/../api</protoSourceRoot>
    <!-- вкладывать *.proto файлы в итоговый билд
    (на мой взгляд удобно, если использовать как библиотеку,
    используемую и в клиенте и в сервисе) -->
    <attachProtoSources>false</attachProtoSources>
</configuration>
```

Для выполнения необходимо добавить настройки:
```xml
<execution>
    <id>compile</id>
    <goals>
        <goal>compile</goal>
        <goal>compile-custom</goal>
    </goals>
    <configuration>
        <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
        <pluginId>grpc-java</pluginId>
        <pluginArtifact>io.grpc:protoc-gen-grpc-java:${java.grpc.version}:exe:${os.detected.classifier}</pluginArtifact>
        <protocPlugins>
            <!-- Плагин для генерации сервиса в котлин -->
            <protocPlugin>
                <id>grpc-kotlin</id>
                <groupId>io.grpc</groupId>
                <artifactId>protoc-gen-grpc-kotlin</artifactId>
                <version>${grpc.kotlin.version}</version>
                <classifier>jdk8</classifier>
                <mainClass>io.grpc.kotlin.generator.GeneratorRunner</mainClass>
            </protocPlugin>
        </protocPlugins>
    </configuration>
</execution>
```

Так же для котлина стоит добавить выполнение:
```xml
<execution>
    <id>compile-kt</id>
    <goals>
        <goal>compile-custom</goal>
    </goals>
    <configuration>
        <protocArtifact>com.google.protobuf:protoc:${protobuf.protoc.version}:exe:${os.detected.classifier}</protocArtifact>
        <outputDirectory>${project.build.directory}/generated-sources/protobuf/kotlin</outputDirectory>
        <pluginId>kotlin</pluginId>
    </configuration>
</execution>
```

[К началу](#grpc)



## Полезные ссылки<a id="links"></a>

* [Реализация gRPC с помощью Java и Spring Boot](https://habr.com/ru/companies/otus/articles/730740/)
* [Kotlin gRPC with Spring](https://dev.to/aleksk1ng/kotlin-grpc-with-spring-9np)
* [gRPC Error Handling](https://www.vinsguru.com/grpc-error-handling/)

[К началу](#grpc)