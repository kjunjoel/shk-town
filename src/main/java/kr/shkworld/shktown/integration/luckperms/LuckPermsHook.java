package kr.shkworld.shktown.integration.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;

import java.util.Optional;

public class LuckPermsHook {
    private LuckPerms luckPermsAPI;

    public boolean hook() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return false;
        }

        this.luckPermsAPI = LuckPermsProvider.get();
        return true;
    }

    public Optional<LuckPerms> getApi() {
        return Optional.ofNullable(luckPermsAPI);
    }
}
