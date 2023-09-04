# Отправка E-Mail

Отправка писем происходит с использованием стартера Spring'а

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

Для интеграционных тестов используется библиотека [GreenMail](https://greenmail-mail-test.github.io/greenmail/)

```xml
<!-- Ядро -->
<dependency>
    <groupId>com.icegreen</groupId>
    <artifactId>greenmail</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>
<!-- Плагин для работы со спринговым контекстом -->
<dependency>
    <groupId>com.icegreen</groupId>
    <artifactId>greenmail-spring</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>
```
