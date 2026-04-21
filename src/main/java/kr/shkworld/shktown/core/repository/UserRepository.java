package kr.shkworld.shktown.core.repository;

import kr.shkworld.shktown.core.model.User;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserRepository {
    /**
     * 유저 정보를 DB에 저장합니다.
     * @param user 유저 객체
     */
    CompletableFuture<Void> saveUser(User user);

    /**
     * 유저 정보를 DB에 동기적으로 저장합니다.
     * @param user 유저 객체
     */
    void saveUserSync(User user);

    /**
     * 유저 정보를 DB에서 불러옵니다.
     * @param uuid 유저의 UUID
     * @return 해당 UUID로 유저가 존재하지 않을 수 있으므로 Optional 유저 객체
     */
    CompletableFuture<Optional<User>> loadUser(UUID uuid);
}
