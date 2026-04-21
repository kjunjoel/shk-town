package kr.shkworld.shktown.core.model.wealth;

import kr.shkworld.shktown.core.model.AccountType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface WealthComponent {
    /**
     * 자산의 고유 명칭을 반환합니다.
     * @return 자산의 고유 명칭
     */
    String getName();

    /**
     * 특정 유저의 자산 가치를 비동기로 계산하여 반환한다.
     * @param uuid 특정 유저의 UUID
     * @return 자산 가치
     */
    CompletableFuture<BigDecimal> getValue(UUID uuid);

    /**
     * 담당하는 계좌 타입 리스트를 반홥합니다.
     * @return 계좌 타입 리스트
     */
    List<AccountType> getTargetTypes();
}
