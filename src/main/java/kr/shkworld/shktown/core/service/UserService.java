package kr.shkworld.shktown.core.service;

import kr.shkworld.shktown.core.model.User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserService {
    /**
     * 유저 데이터를 캐시에서 가져옵니다.
     * @param uuid 유저의 UUID
     * @return 유저 객체
     */
    User getUserFromCache(UUID uuid);

    /**
     * 유저 데이터를 불러와서 캐싱합니다.
     * @param uuid 유저의 UUID
     * @return 비동기로 가져온 User 객체의 Optional 값
     */
    CompletableFuture<Optional<User>> getUserAsync(UUID uuid);

    /**
     * 유저의 이름을 바꿉니다.
     * @param uuid 유저의 UUID
     * @param name 바꿀 이름
     */
    void updateName(UUID uuid, String name);

    /**
     * 유저의 소속을 바꿉니다.
     * @param uuid 유저의 UUID
     * @param townID 바꿀 마을 ID
     * @param nationID 바꿀 국가 ID
     */
    void updateAffiliation(UUID uuid, long townID, long nationID);

    /**
     * 유저를 DB에 저장합니다.
     * @param user 유저 객체
     */
    CompletableFuture<Void> saveUser(User user);

    /**
     * 모든 정보를 동기적으로 저장합니다.
     */
    void saveAllSync();

    void loadUser(User user);
    void unloadUser(UUID uuid);
    Map<UUID, User> getOnlineUsers();
}