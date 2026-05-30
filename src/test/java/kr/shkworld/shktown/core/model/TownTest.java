package kr.shkworld.shktown.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TownTest {

    @Test
    @DisplayName("생성자 테스트")
    void constructor() {
        UUID townUUID = UUID.randomUUID();
        UUID mayorUUID = UUID.randomUUID();

        Town town = new Town(
                1L,
                townUUID,
                "상어마을",
                mayorUUID,
                10L
        );

        assertEquals(1L, town.getID());
        assertEquals(townUUID, town.getUUID());
        assertEquals("상어마을", town.getName());
        assertEquals(mayorUUID, town.getMayorUUID());
        assertEquals(10L, town.getNationID());

        assertNotNull(town.getMembers());
        assertTrue(town.getMembers().isEmpty());
    }

    @Test
    @DisplayName("withID는 새로운 객체를 생성한다")
    void withID_createsNewObject() {
        Town original = new Town(
                1L,
                UUID.randomUUID(),
                "상어마을",
                UUID.randomUUID(),
                10L
        );

        Town copied = original.withID(999L);

        assertNotSame(original, copied);
    }

    @Test
    @DisplayName("withID는 ID만 변경하고 나머지 값은 유지한다")
    void withID_changesOnlyId() {
        UUID townUUID = UUID.randomUUID();
        UUID mayorUUID = UUID.randomUUID();

        Town original = new Town(
                1L,
                townUUID,
                "상어마을",
                mayorUUID,
                10L
        );

        Town copied = original.withID(999L);

        assertEquals(999L, copied.getID());

        assertEquals(original.getUUID(), copied.getUUID());
        assertEquals(original.getName(), copied.getName());
        assertEquals(original.getMayorUUID(), copied.getMayorUUID());
        assertEquals(original.getNationID(), copied.getNationID());
    }

    @Test
    @DisplayName("이름 변경")
    void setName() {
        Town town = new Town(
                1L,
                UUID.randomUUID(),
                "상어마을",
                UUID.randomUUID(),
                10L
        );

        town.setName("상어뉴타운");

        assertEquals("상어뉴타운", town.getName());
    }

    @Test
    @DisplayName("시장 변경")
    void setMayorUUID() {
        Town town = new Town(
                1L,
                UUID.randomUUID(),
                "상어마을",
                UUID.randomUUID(),
                10L
        );

        UUID newMayor = UUID.randomUUID();

        town.setMayorUUID(newMayor);

        assertEquals(newMayor, town.getMayorUUID());
    }

    @Test
    @DisplayName("국가 변경")
    void setNationID() {
        Town town = new Town(
                1L,
                UUID.randomUUID(),
                "상어마을",
                UUID.randomUUID(),
                10L
        );

        town.setNationID(999L);

        assertEquals(999L, town.getNationID());
    }

    @Test
    @DisplayName("멤버 목록 설정")
    void setMembers() {
        Town town = new Town(
                1L,
                UUID.randomUUID(),
                "상어마을",
                UUID.randomUUID(),
                10L
        );

        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();

        List<UUID> members = List.of(member1, member2);

        town.setMembers(members);

        assertEquals(2, town.getMembers().size());
        assertTrue(town.getMembers().contains(member1));
        assertTrue(town.getMembers().contains(member2));
    }

    @Test
    @DisplayName("빈 멤버 목록 설정")
    void setEmptyMembers() {
        Town town = new Town(
                1L,
                UUID.randomUUID(),
                "상어마을",
                UUID.randomUUID(),
                10L
        );

        town.setMembers(List.of());

        assertTrue(town.getMembers().isEmpty());
    }
}