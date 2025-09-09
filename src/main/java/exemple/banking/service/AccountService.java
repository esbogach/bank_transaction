package exemple.banking.service;


import exemple.banking.dto.Account;
import exemple.banking.exception.InsufficientFundsException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Log4j2
public class AccountService {

    @Getter
    private final List<Account> accounts;
    private final Lock lock = new ReentrantLock();

    public AccountService() {
        this.accounts = new CopyOnWriteArrayList<>();
    }

    public void addAccount(Account account) {
        accounts.add(account);
        log.info("Аккаунт создан: {}", account);
    }

    public Account getAccountById(String id) {
        return accounts.stream()
                .filter(acc -> acc.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void transfer(Account from, Account to, BigInteger amount, int transactionNumber) {
        lock.lock();
        try {
            if (from.getMoney().compareTo(amount) < 0) {
                String errorMsg = String.format("Недостаточно средств. Счет: %s, баланс: %s, сумма: %s",
                        from.getId(), from.getMoney(), amount);
                log.error(errorMsg);
                throw new InsufficientFundsException(errorMsg);
            }

            from.setMoney(from.getMoney().subtract(amount));
            to.setMoney(to.getMoney().add(amount));

            log.info("Транзакция #{}. Перевод {} из {} в {}. Состояние счёта: {}=[{}], {}=[{}]",
                    transactionNumber,
                    amount,
                    from.getId(),
                    to.getId(),
                    from.getId(), from.getMoney(),
                    to.getId(), to.getMoney());

        } finally {
            lock.unlock();
        }
    }

    public BigInteger getTotalBalance() {
        return accounts.stream()
                .map(Account::getMoney)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }
}
