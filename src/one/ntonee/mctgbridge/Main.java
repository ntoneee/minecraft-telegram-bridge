package one.ntonee.mctgbridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import one.ntonee.mctgbridge.ActionListener;

public class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        TelegramApi tg = new TelegramApi(this.getConfig());
        Bukkit.getLogger().info("onEnable " + this.getName());
        getServer().getPluginManager().registerEvents(new ActionListener(tg), this);
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("on Disable " + this.getName());
    }
}
