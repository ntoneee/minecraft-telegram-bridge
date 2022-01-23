package one.ntonee.mctgbridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private TelegramApi tg;
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        tg = new TelegramApi(this.getConfig());
        tg.sendMessage("✅ Сервер запущен!");
        Bukkit.getLogger().info("onEnable " + this.getName());
        getServer().getPluginManager().registerEvents(new ActionListener(tg), this);
    }

    @Override
    public void onDisable() {
        tg.sendMessage("❌ Сервер остановлен!");
        Bukkit.getLogger().info("on Disable " + this.getName());
    }
}
