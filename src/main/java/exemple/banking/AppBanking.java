package exemple.banking;

import exemple.banking.manager.AccountManager;
import exemple.banking.service.AccountService;
import exemple.banking.service.TransferService;
import exemple.banking.task.TransferTask;
import lombok.extern.log4j.Log4j2;

import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Log4j2
public class AppBanking {
    private static final int ACCOUNT_COUNT = 9;
    private static final int THREAD_COUNT = 90;
    private static final int MAX_TRANSACTIONS = 30;
    private static final BigInteger INITIAL_BALANCE = new BigInteger("10000");


    static AccountService accountService = new AccountService();
    static AccountManager accountManager = new AccountManager(accountService);
    static TransferService transferService = new TransferService(accountService);

    public static void main(String[] args) {
        log.info("Запуск приложения ...");

        // Инициализация аккаунтов
        accountManager.initializeAccounts(ACCOUNT_COUNT, INITIAL_BALANCE);

        // Вывод начального баланса
        log.info("Первоначальный общий баланс: {}", accountService.getTotalBalance());

        // Создание и запуск потоков
        try (ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT)) {
            for (int i = 1; i <= THREAD_COUNT; i++) {
                executor.execute(new TransferTask(transferService, "Thread-" + i, MAX_TRANSACTIONS));
            }

            executor.shutdown();

            // Ожидание завершения всех задач
            if (!executor.awaitTermination(2, TimeUnit.MINUTES)) {
                log.warn("Тайм-аут достигнут, принудительно  останавливаем");
                executor.shutdownNow();
                if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                    log.error("Исполнитель не прекратил");
                }
            }

        } catch (InterruptedException e) {
            log.error("Основная нить прервана: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }

        // Вывод финального баланса
        logTotalBalance();
    }


    private static void logTotalBalance() {
        BigInteger total = accountService.getTotalBalance();
        BigInteger expectedTotal = INITIAL_BALANCE.multiply(BigInteger.valueOf(ACCOUNT_COUNT));

        log.info("Окончательный общий баланс: {}", total);
        log.info("Ожидаемый общий баланс: {}", expectedTotal);

        if (total.compareTo(expectedTotal) == 0) {
            log.info("Проверка сохранения баланса: ПРОЙДЕНА");
        } else {
            log.error("Проверка сохранения баланса: не удалось. Разница: {}",
                    total.subtract(expectedTotal));
        }
    }
}
