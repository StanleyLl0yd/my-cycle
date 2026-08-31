# Первый релиз «Мой Цикл» в RuStore — чек-лист

## Релизные файлы

- [ ] Использовать `My-Cycle-v1.1.3.aab` из официального GitHub Release v1.1.3.
- [ ] Проверить package: `com.silverlightning.mycycle`.
- [ ] Проверить versionName: `1.1.3`, versionCode: `5`.
- [ ] Загрузить `My-Cycle-v1.1.3-upload-cert.pem` как сертификат upload key, если RuStore Console запрашивает его на этапе App Signing.
- [ ] Подготовить `pepk_out.zip` с помощью `scripts/export-rustore-pepk.sh` и уникального encryption key из RuStore Console.
- [ ] Использовать `store/rustore/icon-512.png` как иконку карточки.
- [ ] Загрузить PNG из `store/rustore/screenshots/` в указанном порядке.

## Карточка

- [ ] Название: `Мой Цикл`.
- [ ] Тип: приложение.
- [ ] Основная категория: `Здоровье и спорт`.
- [ ] Вторую категорию не указывать.
- [ ] Рекомендуемый возрастной рейтинг: `12+`.
- [ ] Цена: бесплатно.
- [ ] Реклама: нет.
- [ ] Встроенные покупки: нет.
- [ ] Скопировать краткое и полное описание из `listing.ru.md`.
- [ ] Скопировать текст «Что нового» из `listing.ru.md`.

## Данные и ссылки

- [ ] В декларации данных указать локальную обработку введённых пользователем данных о здоровье.
- [ ] Передача разработчику: нет.
- [ ] Передача третьим лицам: нет.
- [ ] Аналитика, реклама и трекеры: нет.
- [ ] Android permissions: отсутствуют, включая `INTERNET`.
- [ ] Политика конфиденциальности: `https://github.com/StanleyLl0yd/my-cycle/blob/main/PRIVACY.ru.md`.
- [ ] Условия использования: `https://github.com/StanleyLl0yd/my-cycle/blob/main/TERMS.ru.md`.
- [ ] Сайт: `https://github.com/StanleyLl0yd/my-cycle`.
- [ ] Поддержка: `https://github.com/StanleyLl0yd/my-cycle/issues`.

## App Signing

- [ ] В RuStore Console получить `pepk.jar` и уникальный encryption key для приложения.
- [ ] Выполнить локально `scripts/export-rustore-pepk.sh` с исходным JKS и encryption key.
- [ ] Загрузить полученный `pepk_out.zip` в RuStore Console.
- [ ] Никогда не добавлять JKS, пароли или encryption key в Git и публичные файлы.

## Перед отправкой

- [ ] Проверить SHA-256 AAB по файлу `.aab.sha256` из GitHub Release v1.1.3.
- [ ] Проверить SHA-256 upload-сертификата: `f02571c40741e2cb071564f5b63fd3dc38a875d0eda11a8c42269ed635bc2a58`.
- [ ] Убедиться, что на скриншотах только реальный интерфейс приложения и нет системных панелей Android.
- [ ] Не использовать Google Drive как публичный источник релиза.
- [ ] Отправить первую версию на модерацию через RuStore Console.

После появления первой активной версии можно подключать RuStore API для автоматизации следующих публикаций.
