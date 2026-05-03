package kr.shkworld.shktown.core.repository;

import kr.shkworld.shktown.core.model.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AccountRepository {
    /**
     * 계좌를 DB에 저장합니다.
     * @param account 계좌 객체
     */
    CompletableFuture<Void> saveAccount(Account account);

    /**
     * 계좌를 DB에 동기적으로 저장합니다.
     * @param account 계좌 객체
     */
    void saveAccountSync(Account account);

    /**
     * 계좌번호로 계좌를 DB에서 불러옵니다.
     * @param accountNumber 계좌번호
     * @return 해당 계좌번호로 계좌가 없을 수 있으므로 Optional 계좌 객체
     */
    CompletableFuture<Optional<Account>> findByNumber(String accountNumber);

    /**
     * 유저의 UUID로 유저가 소유한 모든 계좌를 불러옵니다.
     * @param uuid 유저의 UUID
     * @return 계좌 목록
     */
    CompletableFuture<List<Account>> findAllByOwner(UUID uuid);
}
