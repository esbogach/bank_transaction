package exemple.banking.manager;

import exemple.banking.dto.Account;
import exemple.banking.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.math.BigInteger;
import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
public class AccountManager {
    private final AccountService accountService;

    public void initializeAccounts(int count, BigInteger initialBalance) {
        for (int i = 0; i < count; i++) {
            String id = "RU-" + UUID.randomUUID().toString().substring(0, 8);
            Account account = new Account(id, initialBalance);
            accountService.addAccount(account);
        }
        log.info("Создание {} счета с начальным балансом {}", count, initialBalance);
    }
}
