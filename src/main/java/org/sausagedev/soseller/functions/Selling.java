package org.sausagedev.soseller.functions;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.sausagedev.soseller.SoSeller;
import org.sausagedev.soseller.configuration.Config;
import org.sausagedev.soseller.configuration.data.MessagesField;
import org.sausagedev.soseller.configuration.data.SettingsField;
import org.sausagedev.soseller.database.DataManager;
import org.sausagedev.soseller.utils.ItemBuilder;
import org.sausagedev.soseller.utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Selling {
    private final SoSeller main = SoSeller.getPlugin();

    public void sellItems(Player p, ItemStack[] items, boolean withMessage, SaleMode mode) {
        DataManager.PlayerData playerData = DataManager.search(p.getUniqueId());
        double boost = playerData.getBoost();
        Map<String, Object> prices = Config.settings().sellItems();

        List<ItemStack> sellableItems = Arrays
                .stream(items)
                .filter((e) -> canSale(e, prices))
                .toList();

        int sumOfSellable = sellableItems
                .stream()
                .mapToInt(ItemStack::getAmount)
                .sum();

        int neededAmount = switch (mode) {
            case ALL -> sumOfSellable;
            case STACK -> Math.min(sumOfSellable, 64);
            case ONE -> Math.min(sumOfSellable, 1);
        };

        if (neededAmount <= 0) //Нечего продавать
            return;

        SaleResult saleResult = new SaleResult(0, 0);
        for (ItemStack item : sellableItems) {
            var currentSale = processSale(
                    item,
                    (Integer) prices.get(item.getType().toString()),//Unsafe?
                    boost,
                    neededAmount
                    );
            neededAmount -= currentSale.amount;
            saleResult = saleResult.add(currentSale);
        }

        if (saleResult.isZero())
            return; //Почему?

        main.getEconomy().depositPlayer(p, saleResult.profit);

        int finalItems = saleResult.amount;
        playerData.addItems(finalItems);
        DataManager.replace(null, playerData);
        Utils.playSound(p, "onSellItems");

        if (withMessage)
            sendMessage(p, saleResult);
    }

    public void sellItem(Player p, ItemStack item, boolean withMessage, SaleMode saleMode) {
        this.sellItems(p, new ItemStack[]{item}, withMessage, saleMode);
    }

    private boolean canSale(ItemStack item, Map<String, Object> prices) {
        if (item == null
                || item.getType().equals(Material.AIR)
                || new ItemBuilder(item).hasFunction())
            return false;
        String key = item.getType().toString();
        return prices.containsKey(key);
    }

    private SaleResult processSale(ItemStack item, int price, double personalBoost, int amount) {
        double profit = price*amount*personalBoost*Config.settings().globalBoost();
        item.setAmount(item.getAmount() - amount);

        return new SaleResult(amount, profit);
    }

    private record SaleResult(int amount, double profit) {
        public SaleResult add(SaleResult o) {
            return new SaleResult(amount+o.amount, profit+o.profit);
        }
        public boolean isZero() {
            return amount == 0 && profit == 0;
        }
    }

    private void sendMessage(Player p, SaleResult saleResult) {
        String msg = Config.messages().sold();
        msg = msg.replace("{amount}", String.valueOf(saleResult.amount))
                .replace("{profit}", String.valueOf(saleResult.profit));
        p.sendMessage(Utils.convert(msg));
    }
}
