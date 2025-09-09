package exemple.banking.service;

import exemple.banking.dto.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

@Log4j2
@RequiredArgsConstructor
public class TransferService {
    private final AccountService accountService;
    private final Random random = new Random();

    public void performRandomTransfer(int transactionNumber) {
        List<Account> accounts = accountService.getAccounts();
        if (accounts.size() < 2) {
            log.warn("Недостаточно учетных записей для передачи");
            return;
        }

        Account fromAccount;
        Account toAccount;

        do {
            fromAccount = accounts.get(random.nextInt(accounts.size()));
            toAccount = accounts.get(random.nextInt(accounts.size()));
        } while (fromAccount.equals(toAccount));

        BigInteger maxAmount = fromAccount.getMoney();
        if (maxAmount.compareTo(BigInteger.ZERO) <= 0) {
            log.warn("Учетная запись {} не имеет достаточно средств для перевода", fromAccount.getId());
            return;
        }

        BigInteger amount = generateRandomAmount(maxAmount);

        try {
            accountService.transfer(fromAccount, toAccount, amount, transactionNumber);
        } catch (Exception e) {
            log.error("Передача не удалась: {}", e.getMessage());
        }
    }

    private BigInteger generateRandomAmount(BigInteger maxAmount) {
        if (maxAmount.compareTo(BigInteger.ONE) <= 0) {
            return BigInteger.ONE;
        }

        // Генерируем случайный процент от 10% до 100% от доступной суммы
        double percentage = 0.1 + (random.nextDouble() * 0.9);

        // Преобразуем BigInteger в double для вычислений
        double maxAmountDouble = maxAmount.doubleValue();
        double amountDouble = maxAmountDouble * percentage;

        // Округляем до целого числа и конвертируем обратно в BigInteger
        long amountLong = Math.round(amountDouble);

        // Убеждаемся, что сумма не меньше 1 и не больше maxAmount
        BigInteger amount = BigInteger.valueOf(Math.max(1, amountLong));

        if (amount.compareTo(maxAmount) > 0) {
            return maxAmount;
        }

        return amount;
    }


}
