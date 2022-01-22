package one.ntonee.mctgbridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import one.ntonee.mctgbridge.ActionListener;

public class Main extends JavaPlugin {
    private TelegramApi tg;
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        tg = new TelegramApi(this.getConfig());
        tg.sendMessage("✅ Сервер запускается!");
        Bukkit.getLogger().info("onEnable " + this.getName());
        getServer().getPluginManager().registerEvents(new ActionListener(tg), this);
    }

    @Override
    public void onDisable() {
        tg.sendMessage("❌ Сервер выключается!");
        Bukkit.getLogger().info("on Disable " + this.getName());
    }
}
