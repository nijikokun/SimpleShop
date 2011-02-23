package com.nijikokun.bukkit.SimpleShop;

import com.nijiko.simpleshop.Messaging;
import com.nijiko.simpleshop.Misc;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijikokun.bukkit.iConomy.iConomy;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;

/*
 * SimpleShop v1.0
 * Copyright (C) 2011  Nijikokun <nijikokun@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class SimpleShop extends JavaPlugin {
    /*
     * Loggery Foggery
     */
    public static final Logger log = Logger.getLogger("Minecraft");

    /*
     * Central Data pertaining directly to the plugin name & versioning.
     */
    public static String name = "SimpleShop";
    public static String codename = "Yen";
    public static String version = "1.3";

    /**
     * Listener for the plugin system.
     */
    public iListen l = new iListen(this);

    /*
     * Controller for permissions and security.
     */
    public static Permissions Permissions;

    /**
     * Miscellaneous object for various functions that don't belong anywhere else
     */
    public static Misc Misc = new Misc();

    /**
     * Internal Properties controllers
     */
    public static iProperty Settings, Items;

    /*
     * Variables
     */
    public static String directory = "SimpleShop" + File.separator;
    public static String server_name = "Alfenia", shop_tag = "&d[&fShop&d]&f ", currency = "Coin";
    public static int max_per_purchase = 64, max_per_sale = 64;
    public static boolean utilize_stock = false;
    public static Server Server;

    /*
     * Database connections
     */
    private String database_type = "flatfile";
    public static String flatfile = directory + "shop.flat";
    public static String sqlite = "jdbc:sqlite:" + directory + "shop.db";
    public static String mysql = "jdbc:mysql://localhost:3306/minecraft";
    public static String mysql_user = "root";
    public static String mysql_pass = "pass";
    public static Timer timer = null;
    public static iConomy iC = null;

    /**
     * Item names
     */
    public static HashMap<String, String> items;

    /*
     * Database connection
     */
    public static Database db = null;

    public SimpleShop(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
        super(pluginLoader, instance, desc, folder, plugin, cLoader);
        folder.mkdir();

        // Database setup
        directory = getDataFolder() + File.separator;
        sqlite = "jdbc:sqlite:" + directory + "shop.db";
        flatfile = directory + "shop.flat";

        registerEvents();

        // Server
        Server = this.getServer();

        log.info(Messaging.bracketize(name) + " version " + Messaging.bracketize(version) + " (" + codename + ") loaded");
    }

    public void onDisable() {
        log.info(Messaging.bracketize(name) + " version " + Messaging.bracketize(version) + " (" + codename + ") disabled");
    }

    public void onEnable() {
        setup();
        setupItems();
        setupCurrency();
        setupPermissions();
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND, l, Priority.Normal, this);
    }

    public void setup() {
        // Properties
        Settings = new iProperty(getDataFolder() + File.separator + "SimpleShop.settings");
        Items = new iProperty("items.db");

        // Shop settings
        shop_tag = Settings.getString("shop-tag", shop_tag);
        max_per_purchase = Settings.getInt("max-items-per-purchase", 64);
        max_per_sale = Settings.getInt("max-items-per-sale", 64);
        utilize_stock = Settings.getBoolean("use-stock", false);

        // Database
        database_type = Settings.getString("database-type", "flatfile");

        // MySQL
        mysql = Settings.getString("mysql-db", mysql);
        mysql_user = Settings.getString("mysql-user", mysql_user);
        mysql_pass = Settings.getString("mysql-pass", mysql_pass);

        // Connect & Create
        if (database_type.equalsIgnoreCase("mysql")) {
            db = new Database(Database.Type.MYSQL);
        } else {
            db = new Database(Database.Type.SQLITE);
        }

        // Server
        Server = this.getServer();
    }

    public void setupPermissions() {
        Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

        if (this.Permissions == null) {
            if (test != null) {
                this.Permissions = (Permissions) test;
            } else {
                log.info(Messaging.bracketize(name) + " Permission system not enabled. Disabling plugin.");
                this.getServer().getPluginManager().disablePlugin(this);
            }
        }
    }

    public void setupItems() {
        Map mappedItems = null;
        items = new HashMap<String, String>();

        try {
            mappedItems = Items.returnMap();
        } catch (Exception ex) {
            log.info(Messaging.bracketize(name + " Flatfile") + " could not grab item list!");
        }

        if (mappedItems != null) {
            for (Object item : mappedItems.keySet()) {
                String id = (String) item;
                String itemName = (String) mappedItems.get(item);
                items.put(id, itemName);
            }
        }
    }

    public void setupCurrency() {
        Plugin test = this.getServer().getPluginManager().getPlugin("iConomy");

        if (test != null) {
            iC = (iConomy) test;
            currency = iC.currency;
        } else {
            log.info(Messaging.bracketize(name) + " iConomy is not loaded. Disabling plugin.");
            this.getPluginLoader().disablePlugin(this);
        }
    }
}
