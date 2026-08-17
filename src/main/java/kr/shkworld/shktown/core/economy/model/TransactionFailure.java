package kr.shkworld.shktown.core.economy.model;

public enum TransactionFailure {
    NONE,
    INVALID_REQUEST,
    INVALID_AMOUNT,
    SAME_ACCOUNT,
    SOURCE_NOT_FOUND,
    TARGET_NOT_FOUND,
    INSUFFICIENT_BALANCE
}
