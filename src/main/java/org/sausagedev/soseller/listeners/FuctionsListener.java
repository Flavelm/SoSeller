package org.sausagedev.soseller.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.sausagedev.soseller.configuration.Config;
import org.sausagedev.soseller.database.DataManager;
import org.sausagedev.soseller.functions.AutoSellModify;
import org.sausagedev.soseller.functions.BoostsModify;
import org.sausagedev.soseller.functions.SaleMode;
import org.sausagedev.soseller.functions.Selling;
import org.sausagedev.soseller.gui.CustomHolder;
import org.sausagedev.soseller.gui.Menu;
import org.sausagedev.soseller.utils.*;

import java.util.Arrays;
import java.util.UUID;

public class FuctionsListener implements Listener {
    private final AutoSellModify autoSellModify = new AutoSellModify();
    private final BoostsModify boostsModify = new BoostsModify();
    private final Selling selling = new Selling();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory clickedInventory = e.getClickedInventory();
        if (clickedInventory != null
            && !(clickedInventory.getHolder() instanceof CustomHolder)) {
            return;
        }

        e.setResult(Event.Result.DENY);

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        ItemBuilder itemBuilder = new ItemBuilder(item);
        if (!itemBuilder.hasFunction()) {
            return;
        }

        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        DataManager.PlayerData playerData = DataManager.search(player.getUniqueId());

        String f = itemBuilder.function().toLowerCase();
        Menu menu = new Menu();
        UUID uuid = player.getUniqueId();
        String currentMenu = MenuDetect.getMenu(uuid) != null ? MenuDetect.getMenu(uuid) : "main";

        if (f.contains("move_to-")) {
            f = f.replace("move_to-", "");
            menu.open(player, f);
            Utils.playSound(player, "onSwapGui");
            return;
        }

        switch (f) {
            case "offon_autosell_items":
                autoSellModify.offOnAutoSellItem(player, item.getType());;
                menu.open(player, currentMenu);
                return;
            case "sell_all":
                boolean withMsg = (boolean) Config.settings().autoSell().get("message");
                selling.sellItems(player, player.getInventory().getStorageContents(), withMsg, SaleMode.ALL);
                menu.open(player, currentMenu);
                return;
            case "modern_sell_all":
                boolean withMsg1 = (boolean) Config.settings().autoSell().get("message");
                var click = e.getClick();
                SaleMode mode;
                if (click.isLeftClick())
                    mode = SaleMode.ONE;
                else if (click.isRightClick() && click.isShiftClick())
                    mode = SaleMode.ALL;
                else if (click.isRightClick())
                    mode = SaleMode.STACK;
                else
                    return;
                selling.sellItems(player, player.getInventory().getStorageContents(), withMsg1, mode);
                menu.open(player, currentMenu);
                return;
            case "buy_boost":
                boostsModify.buyBoost(player);
                menu.open(player, currentMenu);
                return;
            case "auto-sell":
                boolean bought = playerData.isAutoSellBought();
                if (bought || (int) Config.settings().autoSell().get("cost") == 0) {
                    boolean isEnabled = AutoSell.isEnabled(uuid);
                    Utils.playSound(player, "onSwapAutoSell");
                    if (isEnabled) {
                        AutoSell.disable(uuid);
                        menu.open(player, currentMenu);
                        return;
                    }
                    AutoSell.enable(uuid);
                    menu.open(player, currentMenu);
                    return;
                }
                autoSellModify.buyAutoSell(player);
                menu.open(player, currentMenu);
            default:
                throw new RuntimeException("Function not found!!");
        }
    }
}