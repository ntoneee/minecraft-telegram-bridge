package one.ntonee.mctgbridge;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private TelegramApi tg;
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        tg = new TelegramApi(this.getConfig(), this);
        if (this.getConfig().getBoolean("bridge-to-telegram.server-state.enable")) {
            tg.sendMessage("✅ Сервер запущен!");
        }
        Bukkit.getLogger().info("onEnable " + this.getName());
        getServer().getPluginManager().registerEvents(new ActionListener(tg, this.getConfig()), this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                tg.actualizeListMessage();
            }
        }, 0, 20);
    }

    @Override
    public void onDisable() {
        if (this.getConfig().getBoolean("bridge-to-telegram.server-state.disable")) {
            tg.sendMessage("❌ Сервер остановлен!");
        }
        tg.setListMessage("❌ Сервер остановлен!");
        Bukkit.getLogger().info("on Disable " + this.getName());
    }
}
