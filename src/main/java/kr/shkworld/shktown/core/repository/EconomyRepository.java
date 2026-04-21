package kr.shkworld.shktown.core.repository;

import kr.shkworld.shktown.core.model.RankEntry;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EconomyRepository {
    /**
     * 여러 계좌 타입을 합산하여 순위 리스트를 반환합니다.
     * @param typeCodes 계좌 타입 코드들
     * @param limit 최대 개수
     * @param offset 몇 위부터 계산할 지(오프셋)
     * @return 비동기로 계산한 순위 리스트
     */
    CompletableFuture<List<RankEntry>> findWealthRanking(List<Integer> typeCodes, int limit, int offset);
}
