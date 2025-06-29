package org.sausagedev.soseller.database;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.*;

public class DataManager {
    static final Map<UUID, PlayerData> dataContainer = new HashMap<>();

    public static Map<UUID, PlayerData> getDataContainer() {
        return dataContainer;
    }

    public static PlayerData search(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        assert p != null;
        return dataContainer.getOrDefault(uuid, new PlayerData(p));
    }

    public static void replace(PlayerData old, PlayerData current) {
        dataContainer.replace(current.uuid, current);
    }

    public static void importData() {
        Database.getUUIDs().forEach(uuid -> {
            PlayerData playerData = new PlayerData(uuid);
            playerData.setItems(Database.getItems(uuid))
                    .setBoost(Database.getBoost(uuid))
                    .setAutoSellBought(Database.isBoughtAutoSell(uuid));
            DataManager.getDataContainer().put(playerData.uuid, playerData);
        });
    }

    public static void exportData() {
        dataContainer.values().forEach(playerData -> {
            UUID uuid = playerData.getUUID();
            Database.register(uuid);
            Database.setItems(uuid, playerData.getItems());
            Database.setBoost(uuid, playerData.getBoost());
            Database.setAutoSellBought(uuid, playerData.isAutoSellBought());
        });
    }

    public static class PlayerData implements Cloneable {
        final UUID uuid;
        int items;
        double boost;
        boolean autoSell;

        public PlayerData(Player p) {
            uuid = p.getUniqueId();
            items = 0;
            boost = 1;
            autoSell = false;
        }

        public PlayerData(UUID uuid) {
            this.uuid = uuid;
            items = 0;
            boost = 1;
            autoSell = false;
        }

        public PlayerData setItems(int items) {
            this.items = items;
            return this;
        }

        public PlayerData addItems(int items) {
            this.items += items;
            return this;
        }

        public PlayerData takeItems(int items) {
            this.items -= items;
            return this;
        }

        public PlayerData setBoost(double boost) {
            this.boost = boost;
            return this;
        }

        public PlayerData addBoost(double boost) {
            DecimalFormat df = new DecimalFormat("#.0");
            this.boost += Double.parseDouble(df.format(boost).replace(',', '.'));
            return this;
        }

        public PlayerData takeBoost(double boost) {
            this.boost -= boost;
            return this;
        }

        public PlayerData setAutoSellBought(boolean autoSell) {
            this.autoSell = autoSell;
            return this;
        }

        public int getItems() {
            return items;
        }

        public double getBoost() {
            DecimalFormat df = new DecimalFormat("#.0");
            return Double.parseDouble(df.format(boost).replace(',', '.'));
        }

        public boolean isAutoSellBought() {
            return autoSell;
        }

        public UUID getUUID() {
            return uuid;
        }

        @Override
        public PlayerData clone() {
            try {
                PlayerData clone = (PlayerData) super.clone();
                // TODO: copy mutable state here, so the clone can't change the internals of the original
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }
}
