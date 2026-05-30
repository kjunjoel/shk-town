package kr.shkworld.shktown.core.economy.wealth;

import kr.shkworld.shktown.core.model.Account;
import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.User;
import kr.shkworld.shktown.core.service.UserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LiquidWealth implements WealthComponent {
    private final UserService userService;

    public LiquidWealth(UserService userService) {
        this.userService = userService;
    }

    @Override
    public String getName() {
        return "유동자산";
    }

    @Override
    public CompletableFuture<BigDecimal> getValue(UUID uuid) {
        return userService.getUserAsync(uuid).thenApply(opt ->
                opt.map(this::calculate).orElse(BigDecimal.ZERO)
        );
    }

    @Override
    public List<AccountType> getTargetTypes() {
        return List.of(AccountType.SAVINGS, AccountType.INVESTMENT);
    }

    private BigDecimal calculate(User user) {
        BigDecimal savings = user.getAccount(AccountType.SAVINGS)
                .map(Account::getBalance)
                .orElse(BigDecimal.ZERO);
        BigDecimal investment = user.getAccount(AccountType.INVESTMENT)
                .map(Account::getBalance)
                .orElse(BigDecimal.ZERO);

        return savings.add(investment);
    }
}
