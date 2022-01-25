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
            tg.sendMessageForce("✅ Сервер запущен!");
        }
        Bukkit.getLogger().info("onEnable " + this.getName());
        tg.actualizeListMessage();
        getServer().getPluginManager().registerEvents(new ActionListener(tg, this.getConfig(), this), this);
    }

    @Override
    public void onDisable() {
        tg.flushMessageBuffer();
        if (this.getConfig().getBoolean("bridge-to-telegram.server-state.disable")) {
            tg.sendMessageForce("❌ Сервер остановлен!");
        }
        tg.setListMessage("❌ " + tg.getPinnedListAnnouncement() + "Сервер остановлен!");
        Bukkit.getLogger().info("on Disable " + this.getName());
    }
}
