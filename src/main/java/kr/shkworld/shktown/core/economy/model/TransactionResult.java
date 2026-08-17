package kr.shkworld.shktown.core.economy.model;

public record TransactionResult(
        boolean success,
        TransactionFailure failure,
        Account sourceAccount,
        Account targetAccount
) {
    public static TransactionResult success(Account sourceAccount, Account targetAccount) {
        return new TransactionResult(true, TransactionFailure.NONE, sourceAccount, targetAccount);
    }

    public static TransactionResult failure(TransactionFailure failure) {
        return new TransactionResult(false, failure, null, null);
    }
}
