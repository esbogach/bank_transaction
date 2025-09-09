package exemple.banking.task;

import exemple.banking.service.TransferService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
@RequiredArgsConstructor
public class TransferTask implements Runnable {
    private static final AtomicInteger transactionCounter = new AtomicInteger(0);
    private final TransferService transferService;
    private final String threadName;
    @Getter
    private final int maxTransactions;
    private final Random random = new Random();


    @Override
    public void run() {
        try {
            while (transactionCounter.get() < maxTransactions) {
                int sleepTime = 1000 + random.nextInt(1000);
                log.debug("Поток {} засыпает {} ms", threadName, sleepTime);
                TimeUnit.MILLISECONDS.sleep(sleepTime);

                if (transactionCounter.incrementAndGet() <= maxTransactions) {
                    transferService.performRandomTransfer(transactionCounter.get());
                }
            }
        } catch (InterruptedException e) {
            log.error("{} прерван: {}", threadName, e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
