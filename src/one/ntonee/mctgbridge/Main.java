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
            tg.sendMessageForce("✅ Сервер запущен!", false);
        }
        Bukkit.getLogger().info("onEnable " + this.getName());
        tg.actualizeListMessage();
        getServer().getPluginManager().registerEvents(new ActionListener(tg, this.getConfig(), this), this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                tg.flushMessageBuffer();
            }
        }, 0, 20);
    }

    @Override
    public void onDisable() {
        tg.flushMessageBuffer();
        if (this.getConfig().getBoolean("bridge-to-telegram.server-state.disable")) {
            tg.sendMessage("❌ Сервер остановлен!");
        }
        tg.setListMessage("❌ " + tg.getPinnedListAnnouncement() + "Сервер остановлен!");
        Bukkit.getLogger().info("on Disable " + this.getName());
    }
}
