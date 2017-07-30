package com.tradingbot.ui;

public class Captions_ru extends Captions {

    static final Object[][] contents_ru = {
            {support, "Помощь"},
            {blankUsernameMsg, "Имя пользователя или пароль не может быть пустым"},
            {invalidUsernameMsg, "Не верные имя пользователя или пароль"},
            {subscriptionMsg, "Убедитесь что Вы имеете активную подписку GeeksToy для вашего Betfair аккаунта"},
            {errorPrefixMsg, "Во время логина возникла ошибка"},
            {errorPostfixMsg, "Обратитесь за помощью к администратору"},
            {username, "Имя пользователя"},
            {usernamePrompt, "Твой Betfair логин"},
            {password, "Пароль"},
            {login, "Вход"},
            {start, "Старт"},
            {stop, "Стоп"},
            {botStarted, "Бот запущен"},
            {botStopped, "Бот остановлен"},
            {subscriptionExpired, "Время действия подписки истекло"},
            {minBalance, "Минимальный требуемый баланс"},
            {activateTrial, "Активировать пробную подписку"},
            {trialActivated, "Пробная подписка активирована"},
            {eventsLog, "Лог событий"},
            {today, "Сегодня"},
            {last7days, "Последние 7 дней"},
            {last30days, "Последние 30 дней"},
            {welcome, "Здравствуйте"},
            {balance, "Баланс"},
            {expireDate, "Подписка истекает"},
            {botStatus, "Статус бота"},
            {active, "Запущен"},
            {notActive, "Не запущен"},
            {profit, "Прибыль"},
            {getDiscount, "Получить скидку"},
            {yourDiscount, "<b>Твоя скидка %s%% на покупку подписки</b>"},
            {discountCode, "Код скидки"},
            {discountExpires, "Скидка истекает"},
            {tradingHistory, "История ставок"},
    };

    @Override
    protected Object[][] getContents() {
        return contents_ru;
    }
}
