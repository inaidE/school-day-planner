# School Day Planner

**School Day Planner** — Android-приложение для школьников, которое помогает удобно вести расписание уроков и составлять список вещей, которые нужно взять с собой на конкретный учебный день.

Приложение разработано на **Kotlin** с использованием **Jetpack Compose** и **Material 3**.

## Возможности

* Просмотр расписания по дням недели
* Редактирование списка уроков для каждого дня
* Добавление и удаление предметов
* Вкладка «Взять с собой» для составления списка вещей
* Привязка вещей к конкретному предмету
* Сохранение расписания и списков после закрытия приложения
* Поддержка светлой и тёмной темы
* Современный интерфейс на Jetpack Compose

## Технологии

* Kotlin
* Jetpack Compose
* Material 3
* Navigation Compose
* SharedPreferences
* Gradle Kotlin DSL

## Минимальные требования

* Android 9.0 Pie
* API 28+

## Структура проекта

```text
app/
 └── src/
     └── main/
         ├── java/com/sfedu/dayplanner/
         │   └── MainActivity.kt
         └── res/
             ├── drawable/
             ├── mipmap-*/
             ├── values/
             └── xml/
```

## Запуск проекта

1. Склонируйте репозиторий:

```bash
git clone https://github.com/inaidE/school-day-planner.git
```

2. Откройте проект в **Android Studio**.

3. Дождитесь синхронизации Gradle.

4. Запустите приложение на эмуляторе или физическом Android-устройстве.

## Сборка через Gradle

Для debug-сборки можно выполнить:

```bash
./gradlew assembleDebug
```

Для Windows:

```bash
gradlew.bat assembleDebug
```

После сборки APK будет находиться в директории:

```text
app/build/outputs/apk/debug/
```