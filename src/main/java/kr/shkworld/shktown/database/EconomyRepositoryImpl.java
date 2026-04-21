package kr.shkworld.shktown.database;

import kr.shkworld.shktown.core.model.RankEntry;
import kr.shkworld.shktown.core.repository.EconomyRepository;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class EconomyRepositoryImpl implements EconomyRepository {
    private final JavaPlugin plugin;

    public EconomyRepositoryImpl(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<List<RankEntry>> findWealthRanking(List<Integer> typeCodes, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            List<RankEntry> rankEntries = new ArrayList<>();
            int size = typeCodes.size();

            String placeholders = String.join(",", Collections.nCopies(size, "?"));
            String sql = "SELECT u.name, SUM(a.balanace) as total_wealth " +
                         "FROM users u JOIN accounts a ON u.uuid = a.owner_uuid " +
                         "WHERE a.account_type IN (" + placeholders + ") " +
                         "GROUP BY u.uuid, u.name " +
                         "ORDER BY total_wealth DESC LIMIT ? OFFSET ?";

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                for (int i = 1; i <= size; i++) {
                    preparedStatement.setInt(i, typeCodes.get(i - 1));
                }
                preparedStatement.setInt(size + 1, limit);
                preparedStatement.setInt(size + 2, offset);

                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        rankEntries.add(new RankEntry(
                                rs.getString("name"),
                                rs.getBigDecimal("total_wealth")
                        ));
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에서 자산 순위를 조회하던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
            return rankEntries;
        });
    }
}
