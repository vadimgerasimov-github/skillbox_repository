
# Search Engine

### 1. Описание проекта

Поисковый движок просматривает веб-страницы одного или нескольких сайтов, чтобы найти и предоставить пользователю именно ту информацию, которую он ищет. Быстрый поиск возможен благодаря индексации - созданию и сохранению в базу данных индексов - связей между словами (в виде исходных форм) и страницами, на которых они встречаются.

- Вкладка Dashboard содержит подробные данные об индексации: Для всех сайтов и для каждого по отдельности: количество проиндексированных страниц и лемм. Для каждого сайта: статус индексации, время последней проверки статуса, сообщение об ошибке, если индексация не была завершена.


![dashboard1](https://github.com/user-attachments/assets/f2e89665-3b3b-42cc-8989-c626a0f20798)


- Запуск и остановка индексации происходят на вкладке Management.


![indexing](https://github.com/user-attachments/assets/71cacf55-bfd5-4b09-a425-6284b51fbeaf)
![stopindexing](https://github.com/user-attachments/assets/27087bbd-6fdc-4638-8f22-3015d0a9b25a)


- Помимо индексации всех страниц, существует возможность индексации отдельной страницы.


![singlepage](https://github.com/user-attachments/assets/8cd6f6dc-3024-4793-b1e1-162876c4dcf7)


- Можно индексировать только те страницы, которые относятся к сайтам, перечисленным в файле конфигурации.


![wrongpage](https://github.com/user-attachments/assets/457a312f-cd2c-4cf4-b934-54cc876095a8)


- Результаты поиска выводятся постранично в порядке убывания релевантности. Каждый результат содержит название сайта и заголовок страницы, сниппет и фавикон.


![pages](https://github.com/user-attachments/assets/b1d452d2-e1d1-463d-aa59-7012b2559bad)


- Информация о релевантности результатов поиска логируется и отображается при запуске через терминал.


![log](https://github.com/user-attachments/assets/b74a20e3-979b-4123-a596-bcaf381d1ba2)


- При нажатии на заголовок страницы, происходит переход по сслыке.


![ links](https://github.com/user-attachments/assets/7602fd11-ec5d-48a0-affb-3138ca7ea72c)


- Если ни одна страница не содержит слов из запроса, выводится сообщение об отсутствии результатов.


![0results](https://github.com/user-attachments/assets/53d94438-3492-4b65-a9bb-29362abb17ec)


- Поиск можно производить по страницам отдельных сайтов или сразу всех.


![3sites](https://github.com/user-attachments/assets/899ecb39-236d-4fdd-b19a-36310f1c8c79)


- Сниппет представляет собой фрагмент текста страницы, содержащий наибольшее количество исходных форм слов из запроса, расположенных ближе всего друг к другу.


![differentsnippets](https://github.com/user-attachments/assets/c5e5a07e-59dc-433a-88ae-5718b6c5ac7f)


- Поиск так же работает с запросами на английском языке.


![english](https://github.com/user-attachments/assets/b6f4295b-2732-46fa-ab26-205ef653ebe9)








### 2. Стек используемых технологий

-  **Язык программирования**: Java
-  **Фреймворк**: Spring Boot
    - **Модули**:
        - Spring Web (для создания веб-приложений)
        - Spring Thymeleaf (для шаблонизации веб-страниц)
        - Spring Data JPA (для работы с базой данных)
* **База данных**:
    - PostgreSQL
* **Система управления миграциями базы данных**:
    - Liquibase
* **HTML парсинг**:
    - Jsoup
* **Морфологический анализ**:
    - Apache Lucene Morphology (включая словари для русского и английского языков)
* **Утилиты**:
    - Lombok (для сокращения кода)
    - Spring Retry (для повторных попыток выполнения операций)
    - Jico (библиотека для работы с изображениями)

### 3. Инструкция по локальному запуску проекта

Для запуска проекта на компьютере должны быть установлены:

- Java Runtime Environment (JRE)
- PostgreSQL Server, pgAdmin

Инструкция:
* Скачать и разархивировать папку search_engine.
* В папке target с помощью текстового редактора открыть файл application.yaml. В полях username, password, url указать имя пользователя, пароль, порт, которые используются в настройках соединения с PostgreSQL на данном компьютере, при необходимости отредактировать список индексируемых сайтов.
* С помощью pgAdmin создать на компьютере базу данных под названием “search_engine”.
* Запустить приложение через терминал с настройками из файла application.yaml командой: 

```java
java -jar .../search_engine/target/SearchEngine-1.0-SNAPSHOT.jar --spring.config.location=file:.../search_engine/target/
```

* В браузере перейти по адресу [http://localhost:8080](http://localhost:8080/)

