package kr.shkworld.shktown.core.repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import kr.shkworld.shktown.core.model.Town;

public interface TownRepository {
    /**
     * 마을 ID를 이용해 마을 객체를 비동기적으로 찾습니다.
     * @param id 마을 ID
     * @return 마을 비동기 Optional 객체
     */
    CompletableFuture<Optional<Town>> findByID(long id);

    /**
     * 마을 UUID를 이용해 마을 객체를 비동기적으로 찾습니다.
     * @param uuid 마을 UUID
     * @return 마을 비동기 Optional 객체
     */
    CompletableFuture<Optional<Town>> findByUUID(String uuid);

    /**
     * 마을 이름을 이용해 마을 객체를 비동기적으로 찾습니다.
     * @param name 마을 이름
     * @return 마을 비동기 Optional 객체
     */
    CompletableFuture<Optional<Town>> findByName(String name);

    /**
     * 마을을 비동기적으로 저장합니다.
     * @param town 마을 객체
     * @return 마을 객체
     */
    CompletableFuture<Town> saveTown(Town town);

    /**
     * 마을을 비동기적으로 삭제합니다.
     * @param id 마을 ID
     */
    CompletableFuture<Void> deleteTown(long id);
}
