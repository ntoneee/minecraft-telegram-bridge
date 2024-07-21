# Minecraft-Telegram Bridge

This repo is archived. Consider taking a look on [vanutp/tgbridge](https://github.com/vanutp/tgbridge) — a feature-rich and better supported mod compatible with several mod loaders.

Этот репозиторий не будет обновляться. Если вы ищете мод для Minecraft со сходным функционалом, обратите внимание на [vanutp/tgbridge](https://github.com/vanutp/tgbridge) — он новее, поддерживается, имеет больший функционал и совместим с несколькими платформами.

## Добро пожаловать! 👋
Этот плагин позволяет интегрировать чат Minecraft в группу Telegram и наоборот. Вот как это работает:

![Пример (игра)](guide/assets/minecraft_hello_world.png)
![Пример (Telegram)](guide/assets/telegram_hello_world.png)

_Плагин не работает с серверами на Forge, Fabric и официальном ядре Minecraft. Он совместим с серверами, которые поддерживают плагины под Spigot, например, Spigot, Paper или Purpur._

## Установка
Плагин совместим с Spigot-совместимыми серверными ядрами, например Spigot, Paper, Purpur.

Скачайте последнюю версию из [Releases](https://github.com/ntoneee/minecraft-telegram-bridge/releases) (файл `minecraft-telegram-bridge-{версия}-all.jar`) и скопируйте в папку `plugins/` вашего сервера. 

## Настройка
Для настройки плагина используются 2 файла в папке `plugins/Minecraft-Telegram_Bridge`:
- `lang.yml` для сообщений бота в Telegram и Minecraft
- `config.yml` для настройки поведения бота

В `config.yml` необходимо задать значения `telegram-token` и `telegram-chat-id` — токен бота и ID чата, с каким связать Minecraft, соответственно.
Подробнее о том, как это сделать, в [гайде](guide/basic_setup.md).

### Расширенная настройка
Для расширенной настройки следуйте инструкциям в файлах `lang.yml` и `config.yml` (на английском языке).

## Сборка плагина из исходного кода
**Убедитесь, что у вас установлена Java версии 17.**

Склонируйте репозиторий, перейдите в папку с плагином и запустите команду в зависимости от ОС:

- `./gradlew shadowJar` на Linux и MacOS
- `.\gradlew.bat shadowJar` на Windows

Пример для Unix:
```shell
git clone https://github.com/ntoneee/minecraft-telegram-bridge.git`
cd minecraft-telegram-bridge
./gradlew shadowJar
```

Если вы ранее не устанавливали Gradle версии 7.4, то `gradlew` установит его за вас. 

Собранный JAR-файл будет расположен по адресу `build/libs/minecraft-telegram-bridge-версия-all.jar`. Название файла зависит от директории, в которую вы склонируете репозиторий.
