package one.ntonee.mctgbridge;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public class Main extends JavaPlugin {
    private TelegramApi tg;
    private LangConfiguration lang;

    public LangConfiguration getLangConfig() {
        return lang;
    }

    public String readResourceFile(String path) {
        InputStream idJSONStream = Objects.requireNonNull(getClass().getResourceAsStream(path));
        InputStreamReader reader = new InputStreamReader(idJSONStream);
        BufferedReader buf = new BufferedReader(reader);
        return buf.lines().collect(Collectors.joining("\n"));
    }

    private FileConfiguration loadLanguageConfig() throws IOException, InvalidConfigurationException {
        File langConfigFile = new File(getDataFolder(), "lang.yml");
        if (!langConfigFile.exists()) {
            langConfigFile.getParentFile().mkdirs();
            String content = readResourceFile("/lang.yml");
            langConfigFile.createNewFile();
            Files.write(Paths.get(langConfigFile.getAbsolutePath()), Collections.singleton(content), StandardCharsets.UTF_8);
        }
        return YamlConfiguration.loadConfiguration(langConfigFile);
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        try {
            this.lang = new LangConfiguration(this.loadLanguageConfig());
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
        tg = new TelegramApi(this.getConfig(), this);
        if (this.getConfig().getBoolean("bridge-to-telegram.server-state.enable")) {
            tg.sendMessageForce(lang.getString("telegram.server-state.enable"));
        }
        tg.actualizeListMessage();
        getServer().getPluginManager().registerEvents(new ActionListener(tg, this.getConfig(), this), this);
    }

    @Override
    public void onDisable() {
        tg.flushMessageBuffer();
        if (this.getConfig().getBoolean("bridge-to-telegram.server-state.disable")) {
            tg.sendMessageForce(lang.getString("telegram.server-state.disable"));
        }
        tg.setListMessage(lang.formatString("telegram.announcement-message.server-disabled",
                "announcement", tg.getPinnedListAnnouncement()));
    }
}
