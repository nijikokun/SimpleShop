package com.nijikokun.bukkit.SimpleShop;

import com.nijiko.simpleshop.CLI;
import com.nijiko.simpleshop.Misc;
import com.nijiko.simpleshop.Messaging;
import com.nijikokun.bukkit.iConomy.iConomy;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import net.minecraft.server.WorldServer;
import org.bukkit.inventory.ItemStack;

/**
 * iListen.java
 * <br /><br />
 * Listens for calls from bukkit, and reacts accordingly.
 *
 * @author Nijikokun <nijikokun@gmail.com>
 */
public class iListen extends PlayerListener {

    private static final Logger log = Logger.getLogger("Minecraft");
    /**
     * Miscellaneous object for various functions that don't belong anywhere else
     */
    public Misc Misc = new Misc();
    public static SimpleShop plugin;
    public WorldServer server;
    public CLI Commands;

    public iListen(SimpleShop instance) {
        plugin = instance;

        Commands = new CLI();
        Commands.add("/shop -help|?", "Shop Help.");
        Commands.add("/shop -list|-l +page", "Shop item list.");
        Commands.add("/shop -buy|-b +item", "Shop Purchase.");
        Commands.add("/shop -sell|-s +item", "Shop Sell.");
        Commands.add("/shop -add|-a +item +buy +sell +stock", "Shop add items.");
        Commands.add("/shop -update|-u +item +buy +sell +stock +type:-1", "Shop update items.");
        Commands.add("/shop -remove|-r +item", "Shop Sell.");
        Commands.add("/shop -check|-c +item", "Item check");
    }

    /**
     * Sends simple condensed help lines to the current player
     */
    private void showSimpleHelp() {
        Messaging.send("&e-----------------------------------------------------");
        Messaging.send("&f " + plugin.name + " (&c" + plugin.codename + "&f)   ");
        Messaging.send("&e-----------------------------------------------------");
        Messaging.send("&f [] Required, () Optional                            ");
        Messaging.send("&e-----------------------------------------------------");
        Messaging.send("&f /shop check [id] - information about item           ");
        Messaging.send("&f /shop list - List items for sell / purchase ");
        Messaging.send("&f /shop sell [id]([:amount]) - Sell an item");
        Messaging.send("&f /shop buy [id]([:amount]) - Purchase an item ");
        Messaging.send("&f /shop remove [id] - information about item");
        Messaging.send("&e-----------------------------------------------------");
    }

    private double get_balance(String name) {
        return iConomy.Bank.getAccount(name).getBalance();
    }

    private void set_balance(String name, double amount) {
        iConomy.Bank.getAccount(name).setBalance(amount);
    }

    private void show_balance(Player player) {
        SimpleShop.iC.l.showBalance(player.getName(), player, true);
    }

    @Override
    public void onPlayerCommand(PlayerChatEvent event) {
        final Player player = event.getPlayer();
        double balance = get_balance(player.getName());
        String message = event.getMessage();

        // Save player.
        Messaging.save(player);

        // Commands
        Commands.save(message);

        // Parsing / Checks
        String base = Commands.base();
        String command = Commands.command();
        ArrayList<Object> variables = Commands.parse();

        if (base != null) {
            if (Misc.is(base, "shop")) {
                if (command == null) {
                        showSimpleHelp(); return;
                }

                if(command.equals("check")) {
                    if(variables.get(0) instanceof Integer) {
                        if(Integer.valueOf((String.valueOf(variables.get(0)))) == 0) {
                            showSimpleHelp(); return;
                        }
                    }

                    String item = String.valueOf(variables.get(0));
                    int itemId = Items.validate(item);
                    int itemType = Items.validateGrabType(item);

                    if (itemId == -1) {
                        Messaging.send(SimpleShop.shop_tag + "&7Invalid item.");
                        return;
                    }

                    int[] data = SimpleShop.db.data(itemId, itemType);

                    if (data[0] == -1) {
                        Messaging.send(SimpleShop.shop_tag + "&7Item currently not for purchase.");
                        return;
                    }

                    Messaging.send(SimpleShop.shop_tag + "Item &d" + Items.name(itemId) + "&f info:");

                    if (data[2] != -1) {
                        Messaging.send("Purchasable: &d[&f" + data[4] + "&d]&f for &d" + data[2] + " " + iConomy.currency);
                    }

                    if (data[3] != -1) {
                        Messaging.send("Sellable: &d[&f" + data[4] + "&d]&f for &d" + data[3] + " " + iConomy.currency);
                    }

                    if (SimpleShop.utilize_stock) {
                        Messaging.send("Current Stock: &d" + data[5]);
                    }

                    return;
                }

                if (Misc.isEither(command, "help", "?")) {
                    showSimpleHelp();
                    return;
                }

                if (Misc.isEither(command, "list", "l")) {
                    ArrayList<int[]> list = SimpleShop.db.list();
                    int per_page = 7;
                    int current_page = Integer.valueOf(String.valueOf(variables.get(0)));
                    current_page = (current_page == 0) ? 1 : current_page;
                    int size = 0;

                    for(int[] data : list) {
                        if (data[2] != -1 && data[3] != -1) {
                            size++;
                        }
                    }

                    int amount_pages = (int)Math.ceil(size / per_page) + 1;
                    int page_values = (current_page - 1) * per_page;
                    int page_show = (current_page - 1) * per_page + per_page;

                    if (list.isEmpty()) {
                        Messaging.send(SimpleShop.shop_tag + "&7No items available.");
                    } else if(current_page > amount_pages) {
                        Messaging.send(SimpleShop.shop_tag + "&7Invalid page number.");
                    } else {
                        Messaging.send(SimpleShop.shop_tag + "&dPage &f#"+current_page+"&d of &f"+amount_pages+" &dpages.");

                        for (int i = page_values; i < page_show; i++) {
                            if(list.size()-1 >= i) {
                                int[] data = list.get(i);

                                if (data[2] == -1 && data[3] == -1) {
                                    continue;
                                }

                                String buy = (data[2] != -1) ? " Buy &d[&f" + data[2] + "&d]&f" : "";
                                String sell = (data[3] != -1) ? " Sell &d[&f" + data[3] + "&d]" : "";
                                String stock = (SimpleShop.utilize_stock && data[5] != 0) ? " Stock &d[&f" + data[5] + "&d]" : "";

                                Messaging.send("Item &d[&f" + Items.name(data[0]) + " &dx &f" + data[4] + "&d]&f" + buy + sell + stock);
                            } else {
                                break;
                            }
                        }
                    }

                    return;
                }

                if (Misc.isEither(command, "buy", "b")) {
                    if (!SimpleShop.Permissions.Security.permission(player, "simpleshop.buy")) {
                        return;
                    }

                    String item = String.valueOf(variables.get(0));
                    int itemId = -1;
                    int itemType = -1;
                    int amount = 1;

                    if (item.contains(":")) {
                        String[] data = item.split(":");

                        try {
                            amount = Integer.valueOf(data[1]);
                        } catch (NumberFormatException e) {
                            amount = 1;
                        }

                        itemId = Items.validate(data[0]);
                        itemType = Items.validateGrabType(data[0]);
                    } else {
                        itemId = Items.validate(item);
                        itemType = Items.validateGrabType(item);
                    }

                    if (itemId == -1) {
                        Messaging.send(SimpleShop.shop_tag + "&7Invalid item.");
                        return;
                    }

                    int[] data = SimpleShop.db.data(itemId, itemType);

                    if (data[0] == -1 || data[2] == -1) {
                        Messaging.send(SimpleShop.shop_tag + "&7Item currently not for sale.");
                        return;
                    }

                    if (amount < 1 || (amount * data[4]) > SimpleShop.max_per_purchase) {
                        Messaging.send(SimpleShop.shop_tag + "&7Amount over max per purchase.");
                        return;
                    }

                    if (balance < (data[2] * amount)) {
                        Messaging.send(SimpleShop.shop_tag + "&7You do not have enough &f" + iConomy.currency + "&7 to do this.");
                        return;
                    }

                    if(SimpleShop.utilize_stock && (data[4]*amount) < data[5]) {
                        Messaging.send(SimpleShop.shop_tag + "&7Currently &f" + Items.name(itemId) + "&7 is low in stock.");
                        return;
                    }

                    set_balance(player.getName(), (balance - (data[2] * amount)));

                    if (itemType == -1) {
                        player.getInventory().addItem(new ItemStack(itemId, (amount * data[4])));
                    } else {
                        player.getInventory().addItem(new ItemStack(itemId, (amount * data[4]), (byte) itemType));
                    }

                    Messaging.send(SimpleShop.shop_tag + "Purchased &d[&f" + (amount * data[4]) + "&d]&f " + Items.name(itemId) + " for &d" + (data[2] * amount) + " " + SimpleShop.iC.currency);
                    show_balance(player);
                }

                if (Misc.isEither(command, "sell", "s")) {
                    if (!SimpleShop.Permissions.Security.permission(player, "simpleshop.sell")) {
                        return;
                    }

                    String item = String.valueOf(variables.get(0));
                    int itemId = -1;
                    int itemType = -1;
                    int amount = 1;

                    if (item.contains(":")) {
                        String[] data = item.split(":");

                        try {
                            amount = Integer.valueOf(data[1]);
                        } catch (NumberFormatException e) {
                            amount = 1;
                        }

                        itemId = Items.validate(data[0]);
                        itemType = Items.validateGrabType(data[0]);
                    } else {
                        itemId = Items.validate(item);
                        itemType = Items.validateGrabType(item);
                    }

                    if (itemId == -1) {
                        Messaging.send(SimpleShop.shop_tag + "&7Invalid item.");
                        return;
                    }

                    int[] data = SimpleShop.db.data(itemId, itemType);

                    if (data[0] == -1 || data[3] == -1) {
                        Messaging.send(SimpleShop.shop_tag + "&7Item currently not for purchase.");
                        return;
                    }

                    if (amount < 1 || (amount * data[4]) > SimpleShop.max_per_sale) {
                        Messaging.send(SimpleShop.shop_tag + "&7Amount over max sell amount.");
                        return;
                    }

                    if (itemType != -1) {
                        if (!Items.has(player, itemId, (amount * data[4]))) {
                            Messaging.send(SimpleShop.shop_tag + "&7You do not have enough " + Items.name(itemId) + " to do this.");
                            return;
                        }
                    } else {
                        if (!Items.has(player, itemId, (amount * data[4]))) {
                            Messaging.send(SimpleShop.shop_tag + "&7You do not have enough " + Items.name(itemId) + " to do this.");
                            return;
                        }
                    }

                    if (SimpleShop.utilize_stock) {
                        SimpleShop.db.update(data[0], data[1], data[1], data[2], data[3], data[4], (data[5] + (amount * data[4])));
                    }

                    Items.remove(player, itemId, (amount * data[4]));
                    set_balance(player.getName(), (balance + (data[3] * amount)));

                    Messaging.send(SimpleShop.shop_tag + "Sold &d[&f" + (amount * data[4]) + "&d]&f " + Items.name(itemId) + " for &d" + (data[3] * amount) + " " + iConomy.currency);

                    if (SimpleShop.utilize_stock) {
                        Messaging.send(SimpleShop.shop_tag + "Current Stock: &f" + (data[5] + (amount * data[4])));
                    }

                    show_balance(player);
                }

                if (Misc.isEither(command, "reload", "r")) {
                    if (!SimpleShop.Permissions.Security.permission(player, "simpleshop.items.reload")) {
                        return;
                    }

                    plugin.onEnable();
                    return;
                }

                if (Misc.isEither(command, "add", "a")) {
                    if (!SimpleShop.Permissions.Security.permission(player, "simpleshop.items.add")) {
                        return;
                    }

                    String item = String.valueOf(variables.get(0));
                    int itemId = -1;
                    int itemType = -1;
                    int amount = 1;
                    int buy = Integer.valueOf((String.valueOf(variables.get(1)) == null) ? "0" : String.valueOf(variables.get(1)));
                    int sell = Integer.valueOf((String.valueOf(variables.get(2)) == null) ? "0" : String.valueOf(variables.get(2)));
                    int stock = Integer.valueOf((String.valueOf(variables.get(3)) == null) ? "0" : String.valueOf(variables.get(3)));

                    if (item.contains(":")) {
                        String[] data = item.split(":");

                        try {
                            amount = Integer.valueOf(data[1]);
                        } catch (NumberFormatException e) {
                            amount = 1;
                        }

                        itemId = Items.validate(data[0]);
                        itemType = Items.validateGrabType(data[0]);
                    } else {
                        itemId = Items.validate(item);
                        itemType = Items.validateGrabType(item);
                    }

                    if (itemId == -1) {
                        Messaging.send(SimpleShop.shop_tag + "&7Invalid item.");
                        return;
                    }

                    if (buy == -1 && sell == -1) {
                        Messaging.send(SimpleShop.shop_tag + "&7Buy & Sell cannot both be -1.");
                        return;
                    }

                    if (amount < 1 || amount > SimpleShop.max_per_sale) {
                        Messaging.send(SimpleShop.shop_tag + "&7Invalid amounts.");
                        return;
                    }

                    if (SimpleShop.utilize_stock && stock < 0) {
                        Messaging.send(SimpleShop.shop_tag + "&7Invalid stock amount.");
                        return;
                    }

                    SimpleShop.db.add(itemId, itemType, buy, sell, amount, stock);
                    Messaging.send(SimpleShop.shop_tag + "Item " + Items.name(itemId) + " added:");
                    
                    if (buy != -1) {
                        Messaging.send("Purchasable: &d[&f" + amount + "&d]&f for &d" + buy + " " + iConomy.currency);
                    }

                    if (sell != -1) {
                        Messaging.send("Sellable: &d[&f" + amount + "&d]&f for &d" + sell + " " + iConomy.currency);
                    }

                    if (SimpleShop.utilize_stock) {
                        Messaging.send("Current Stock: &d" + stock);
                    }

                    return;
                }

                if (Misc.isEither(command, "update", "u")) {
                    if (!SimpleShop.Permissions.Security.permission(player, "simpleshop.items.update")) {
                        return;
                    }

                    String item = String.valueOf(variables.get(0));
                    int itemId = -1;
                    int itemType = -1;
                    int oldType = Integer.valueOf(String.valueOf(variables.get(4)));
                    int amount = 1;
                    int buy = Integer.valueOf(String.valueOf(variables.get(1)));
                    int sell = Integer.valueOf(String.valueOf(variables.get(2)));
                    int stock = Integer.valueOf((String.valueOf(variables.get(3)) == null) ? "0" : String.valueOf(variables.get(3)));

                    if (item.contains(":")) {
                        String[] data = item.split(":");

                        try {
                            amount = Integer.valueOf(data[1]);
                        } catch (NumberFormatException e) {
                            amount = 1;
                        }

                        itemId = Items.validate(data[0]);
                        itemType = Items.validateGrabType(data[0]);
                    } else {
                        itemId = Items.validate(item);
                        itemType = Items.validateGrabType(item);
                    }

                    if (itemId == -1) {
                        Messaging.send(SimpleShop.shop_tag + "&7Invalid item.");
                        return;
                    }

                    if (buy == -1 && sell == -1) {
                        Messaging.send(SimpleShop.shop_tag + "&7Buy & Sell cannot both be -1.");
                        return;
                    }

                    if (amount < 1 || amount > SimpleShop.max_per_sale) {
                        Messaging.send(SimpleShop.shop_tag + "&7Invalid amounts.");
                        return;
                    }

                    if (SimpleShop.utilize_stock && stock < 0) {
                        Messaging.send(SimpleShop.shop_tag + "&7Invalid stock amount.");
                        return;
                    }

                    SimpleShop.db.update(itemId, oldType, itemType, buy, sell, amount, stock);
                    Messaging.send(SimpleShop.shop_tag + "Item " + Items.name(itemId) + " updated:");

                     if (buy != -1) {
                        Messaging.send("Purchasable: &d[&f" + amount + "&d]&f for &d" + buy + " " + iConomy.currency);
                    }

                    if (sell != -1) {
                        Messaging.send("Sellable: &d[&f" + amount + "&d]&f for &d" + sell + " " + iConomy.currency);
                    }

                    if (SimpleShop.utilize_stock) {
                        Messaging.send("Current Stock: &d" + stock);
                    }
                    return;
                }

                if (Misc.isEither(command, "remove", "rm")) {
                    if (!SimpleShop.Permissions.Security.permission(player, "simpleshop.items.remove")) {
                        return;
                    }

                    String item = String.valueOf(variables.get(0));
                    int itemId = Items.validate(item);
                    int itemType = Items.validateGrabType(item);

                    if (itemId == -1) {
                        Messaging.send(SimpleShop.shop_tag + "&7Invalid item.");
                        return;
                    }

                    SimpleShop.db.remove(itemId);
                    Messaging.send(SimpleShop.shop_tag + "Item(s) " + Items.name(itemId) + " was removed.");
                    return;
                }
            }
        }
    }
}
