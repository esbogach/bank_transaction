package exemple.singl;


import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Log4j2
public class AppBanking {


    private static final int NUM_ACCOUNTS = 9;
    private static final int NUM_THREADS = 9;
    private static final int MAX_TRANSACTIONS = 30;
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("10000.00");

    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Lock> accountLocks = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private volatile int transactionCount = 0;

    public static void main(String[] args) {
        AppBanking app = new AppBanking();
        app.run();
    }

    public void run() {
        try {
            log.info("Начало перевода денег");
            initializeAccounts();

            ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

            for (int i = 0; i < NUM_THREADS; i++) {
                executor.submit(this::transferWorker);
            }

            executor.shutdown();

            // Ждем завершения всех потоков или достижения лимита транзакций
            while (!executor.isTerminated() && transactionCount < MAX_TRANSACTIONS) {
                Thread.sleep(100);
            }

            executor.shutdownNow();
            logTotalBalance();
            log.info("Заявление заполнено успешно");

        } catch (Exception e) {
            log.error("Критическая ошибка в приложении: {}", e.getMessage(), e);
        }
    }

    private void initializeAccounts() {
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            String id = "VIP-" + UUID.randomUUID().toString().substring(0, 8);
            Account account = new Account(id, INITIAL_BALANCE);
            accounts.put(id, account);
            accountLocks.put(id, new ReentrantLock());
            log.info("Созданная учетная запись: {} с балансом: {}", id, INITIAL_BALANCE);
        }
        log.info("Общий начальный баланс: {}", getTotalBalance());
    }

    private void transferWorker() {
        while (transactionCount < MAX_TRANSACTIONS) {
            try {
                int sleepTime = 1000 + random.nextInt(1000);
                Thread.sleep(sleepTime);

                performRandomTransfer();

            } catch (InterruptedException e) {
                log.warn("Thread прерван: {}", e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Неожиданная ошибка у Thread: {}", e.getMessage(), e);
            }
        }
    }

    private void performRandomTransfer() {
        if (transactionCount >= MAX_TRANSACTIONS) {
            return;
        }

        List<String> accountIds = new ArrayList<>(accounts.keySet());
        if (accountIds.size() < 2) {
            return;
        }

        String fromAccountId = accountIds.get(random.nextInt(accountIds.size()));
        String toAccountId;
        do {
            toAccountId = accountIds.get(random.nextInt(accountIds.size()));
        } while (fromAccountId.equals(toAccountId));

        // Упорядочиваем блокировки для предотвращения deadlock
        String firstLock = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
        String secondLock = fromAccountId.compareTo(toAccountId) < 0 ? toAccountId : fromAccountId;

        Lock first = accountLocks.get(firstLock);
        Lock second = accountLocks.get(secondLock);

        if (first.tryLock()) {
            try {
                if (second.tryLock()) {
                    try {
                        Account fromAccount = accounts.get(fromAccountId);
                        Account toAccount = accounts.get(toAccountId);


                        BigDecimal maxTransferAmount = fromAccount.getMoney();
                        if (maxTransferAmount.compareTo(BigDecimal.ZERO) <= 0) {
                            log.warn("Учетная запись {} имеет недостаточно средств", fromAccountId);
                            return;
                        }

                        BigDecimal transferAmount = generateRandomAmount(maxTransferAmount);

                        if (transferAmount.compareTo(BigDecimal.ZERO) > 0) {
                            performTransfer(fromAccount, toAccount, transferAmount);
                            transactionCount++;
                        }

                    } finally {
                        second.unlock();
                    }
                }
            } finally {
                first.unlock();
            }
        }
    }

    private BigDecimal generateRandomAmount(BigDecimal maxAmount) {
        // Генерируем сумму от 1 до maxAmount
        double percentage = 0.1 + (random.nextDouble() * 0.9); // 10-100% от доступной суммы
        BigDecimal amount = maxAmount.multiply(BigDecimal.valueOf(percentage))
                .setScale(2, RoundingMode.HALF_UP);

        return amount.compareTo(BigDecimal.ZERO) > 0 ? amount : BigDecimal.ONE;
    }

    private void performTransfer(Account fromAccount, Account toAccount, BigDecimal amount) {
        if (fromAccount.getMoney().compareTo(amount) < 0) {
            log.warn("Неудачная транзакция: недостаточно средств на счете {}", fromAccount.getId());
            return;
        }

        fromAccount.setMoney(fromAccount.getMoney().subtract(amount));
        toAccount.setMoney(toAccount.getMoney().add(amount));

        log.info("Транзакция #{}. Перевод {} из {} в {}. Состояние счёта: {}=[{}], {}=[{}]",
                transactionCount + 1,
                amount,
                fromAccount.getId(),
                toAccount.getId(),
                fromAccount.getId(), fromAccount.getMoney(),
                toAccount.getId(), toAccount.getMoney());
    }

    private BigDecimal getTotalBalance() {
        return accounts.values().stream()
                .map(Account::getMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void logTotalBalance() {
        BigDecimal total = getTotalBalance();
        BigDecimal expectedTotal = INITIAL_BALANCE.multiply(BigDecimal.valueOf(NUM_ACCOUNTS));

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
