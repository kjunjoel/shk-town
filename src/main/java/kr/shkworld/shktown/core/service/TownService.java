package kr.shkworld.shktown.core.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import kr.shkworld.shktown.core.model.Town;

public interface TownService {
    /**
     * 마을을 생성합니다.
     * @param name 마을 이름
     * @param mayor 마을 시장의 UUID
     * @return 마을 성공 생성 여부
     */
    CompletableFuture<Boolean> createTown(String name, UUID mayor);

    /**
     * 마을을 찾습니다.
     * @param id 마을 ID
     * @return 마을 객체
     */
    CompletableFuture<Optional<Town>> getTown(long id);

    /**
     * UUID로 마을을 찾습니다.
     * @param uuid 마을 UUID
     * @return 마을 객체
     */
    CompletableFuture<Optional<Town>> getTown(UUID uuid);

    /**
     * 이름으로 마을을 찾습니다.
     * @param name 마을 이름
     * @return 마을 객체
     */
    CompletableFuture<Optional<Town>> getTown(String name);
    
    /**
     * 마을을 찾습니다.
     * @param id 마을 ID
     * @return 마을원이 포함된 마을 객체
     */
    CompletableFuture<Optional<Town>> getTownWithMembers(long id);

    /**
     * UUID로 마을을 찾습니다.
     * @param uuid 마을 UUID
     * @return 마을원이 포함된 마을 객체
     */
    CompletableFuture<Optional<Town>> getTownWithMembers(UUID uuid);

    /**
     * 이름으로 마을을 찾습니다.
     * @param name 마을 이름
     * @return 마을원이 포함된 마을 객체
     */
    CompletableFuture<Optional<Town>> getTownWithMembers(String name);
}
