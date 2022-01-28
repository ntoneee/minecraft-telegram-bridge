package one.ntonee.mctgbridge;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

record CustomStringFormatter(Map<String, String> values) {
    static final Pattern REGEX = Pattern.compile("\\\\(.)|(\\{(\\w+)})");

    String replace(String input) {
        return REGEX.matcher(input).replaceAll(match -> {
            if (match.group(1) != null) { // \-escaped character
                return match.group(1);
            }
            // Leave value as was if not found in Map
            return values.getOrDefault(match.group(3), match.group(2));
        });
    }
}

public class LangConfiguration {
    private final FileConfiguration langConfig;
    private final Map<String, String> minecraftColorMap;

    LangConfiguration(FileConfiguration config) {
        this.langConfig = config;
        minecraftColorMap = new HashMap<>();
        for (ChatColor color : ChatColor.values()) {
            minecraftColorMap.put("color" + color.getChar(), String.valueOf(color));
        }
    }

    public String getString(String path) {
        return Objects.requireNonNull(langConfig.getString(path), "Incomplete lang.yml: " + path);
    }

    public String getMessageMetaString(String path) {
        return getString("minecraft.message-meta." + path) + " ";
    }

    CustomStringFormatter makeSubstitutor(Map<String, String> values, boolean includeColors) {
        HashMap<String, String> mutableValues = new HashMap<>(values);
        if (includeColors) {
            mutableValues.putAll(minecraftColorMap);
        }
        return new CustomStringFormatter(mutableValues);
    }

    public String formatString(String path, Map<String, String> values) {
        return Objects.requireNonNull(makeSubstitutor(values, path.startsWith("minecraft")).replace(getString(path)));
    }

    public String formatString(String path, String key, String value) {
        return formatString(path, Map.of(key, value));
    }

    public String formatMessageMetaString(String path, HashMap<String, String> values) {
        return formatString("minecraft.message-meta." + path, values) + " ";
    }

    public String formatMessageMetaString(String path, String key, String value) {
        return formatMessageMetaString(path, new HashMap<>(Map.of(key, value))) + " ";
    }
}
