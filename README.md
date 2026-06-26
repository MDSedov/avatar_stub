# avatar-stub

Spring Boot заглушка внешней системы для нагрузочного теста загрузки аватарок.

## Что делает

- Работает на порту `7070`.
- На любой REST-запрос, кроме `/admin/**`, возвращает JPEG-картинку байтами.
- Один запрос = одна аватарка из пула.
- Пул аватарок генерируется заранее.
- В серверном режиме картинки не генерируются на лету.
- Размер картинки: `550x550`.
- Формат: `jpeg`.
- Целевой размер файла: около `40 KB`.

## Требования

- Java 21.
- Maven 3.8+.

## Сборка

```bash
mvn clean package
```

## Режим 1. Генерация пула перед тестом

```bash
java -jar target/avatar-stub-1.0.0.jar \
  --spring.main.web-application-type=none \
  --avatar.mode=generate \
  --avatar.generate-count=230000 \
  --avatar.dir=./data/avatars
```

Или скриптом:

```bash
./scripts/generate.sh 230000 ./data/avatars
```

После генерации приложение завершится.

## Режим 2. Запуск заглушки

```bash
java -jar target/avatar-stub-1.0.0.jar \
  --avatar.mode=server \
  --avatar.dir=./data/avatars
```

Или скриптом:

```bash
./scripts/run-server.sh ./data/avatars
```

## Проверка

Статус:

```bash
curl http://localhost:7070/admin/status
```

Пример ответа:

```json
{
  "poolSize": 230000,
  "nextIndex": 12345
}
```

Получить аватарку:

```bash
curl -o avatar.jpg http://localhost:7070/api/users/123/avatar
```

Любой другой REST-запрос тоже вернет JPEG:

```bash
curl -X POST -o avatar.jpg http://localhost:7070/some/external/api
```

Запросы на неизвестные `/admin/**` не отдают аватарки и возвращают `404`.

## Перезагрузка списка файлов

Если аватарки добавлены или удалены вручную:

```bash
curl -X POST http://localhost:7070/admin/reload
```

## Нагрузочный смысл

При 230 000 аватарок и 30 rps прогон займет примерно:

```text
230000 / 30 = 7666 секунд ≈ 2 часа 8 минут
```

Оценка объема:

```text
230000 * 40 KB ≈ 9.2 GB JPEG
Base64 даст примерно +33%, то есть около 12.2 GB данных до накладных расходов Postgres.
```

Смотреть стоит не только rps, но и:

- latency получения аватарки;
- latency base64-преобразования;
- скорость вставки в Postgres;
- рост WAL;
- размер таблицы и TOAST;
- ошибки приложения при длительном прогоне.
