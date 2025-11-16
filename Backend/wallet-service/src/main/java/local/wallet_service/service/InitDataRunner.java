package local.wallet_service.service;


import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import local.wallet_service.model.PlatformWallet;
import local.wallet_service.repository.PlatformWalletRepository;

@Component
@RequiredArgsConstructor
public class InitDataRunner implements CommandLineRunner {

    private final PlatformWalletRepository platformRepo;

    @Value("${wallet.platform.id:1}")
    private Long platformWalletId;

    @Override
    public void run(String... args) {
        platformRepo.findById(platformWalletId).orElseGet(() -> {
            PlatformWallet wallet = PlatformWallet.builder()
                    .id(platformWalletId)
                    .balance(BigDecimal.ZERO)
                    .build();
            return platformRepo.save(wallet);
        });
    }
}
