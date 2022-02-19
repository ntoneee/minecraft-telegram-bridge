package one.ntonee.mctgbridge;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

enum StringFormatterState {
    TEXT,
    OPENED_VARIABLE,
    ESCAPING_CHARACTER
}

record CustomStringFormatter(Map<String, LangSubstitutionValue> values, boolean includeColors) {
    ArrayList<TextComponent> replace(String input) {
        StringFormatterState curState = StringFormatterState.TEXT;
        StringBuilder lastTextComponent = new StringBuilder();
        StringBuilder varName = new StringBuilder();

        TextComponent formatting = new TextComponent("This text component is used only for color propagation");

        ArrayList<TextComponent> result = new ArrayList<>();

        for (int i = 0; i < input.length(); ++i) {
            char now = input.charAt(i);

            if (curState == StringFormatterState.ESCAPING_CHARACTER) {
                curState = StringFormatterState.TEXT;
                lastTextComponent.append(now);
            }
            else if (curState == StringFormatterState.TEXT) {
                if (now == '\\') {
                    curState = StringFormatterState.ESCAPING_CHARACTER;
                }
                else if (now == '{') {
                    curState = StringFormatterState.OPENED_VARIABLE;
                    TextComponent component = new TextComponent(lastTextComponent.toString());
                    component.copyFormatting(formatting);
                    result.add(component);
                    lastTextComponent = new StringBuilder();
                }
                else {
                    lastTextComponent.append(now);
                }
            }
            else {  // opened variable
                if (now == '}') {
                    curState = StringFormatterState.TEXT;
                    String varNameString = varName.toString();
                    varName = new StringBuilder();
                    if (includeColors && varNameString.startsWith("color") && varNameString.length() == 6) {
                        char colorCode = varNameString.charAt(5);
                        ChatColor color = ChatColor.getByChar(colorCode);
                        if ('0' <= colorCode && colorCode <= '9' || 'a' <= colorCode && colorCode <= 'f') {
                            formatting.setColor(color);
                        }
                        else {
                            if (ChatColor.BOLD.equals(color)) {
                                formatting.setBold(true);
                            }
                            if (ChatColor.ITALIC.equals(color)) {
                                formatting.setItalic(true);
                            }
                            if (ChatColor.UNDERLINE.equals(color)) {
                                formatting.setUnderlined(true);
                            }
                            if (ChatColor.STRIKETHROUGH.equals(color)) {
                                formatting.setStrikethrough(true);
                            }
                            if (ChatColor.RESET.equals(color)) {
                                formatting.setColor(null);
                                formatting.setBold(null);
                                formatting.setItalic(null);
                                formatting.setUnderlined(null);
                                formatting.setStrikethrough(null);
                            }
                        }
                    }
                    else {
                        ArrayList<TextComponent> components = values.get(varNameString).getComponents();
                        for (TextComponent component : components) {
                            component.copyFormatting(formatting, false);
                        }
                        result.addAll(components);
                    }
                }
                else {
                    varName.append(now);
                }
            }
        }
        if (!lastTextComponent.isEmpty()) {
            result.add(new TextComponent(lastTextComponent.toString()));
        }
        return result;
    }
}

public class LangConfiguration {
    private final FileConfiguration langConfig;

    LangConfiguration(FileConfiguration config) {
        this.langConfig = config;
    }

    public String getString(String path) {
        return Objects.requireNonNull(langConfig.getString(path), "Incomplete lang.yml: " + path);
    }

    public String getMessageMetaString(String path) {
        return getString("minecraft.message-meta." + path) + " ";
    }

    CustomStringFormatter makeSubstitutor(Map<String, LangSubstitutionValue> values, boolean includeColors) {
        return new CustomStringFormatter(values, includeColors);
    }

    private String concatTextComponents(ArrayList<TextComponent> components) {
        StringBuilder builder = new StringBuilder();
        components.forEach((TextComponent component) -> {
            builder.append(component.getText());
        });
        return builder.toString();
    }

    private Map<String, LangSubstitutionValue> convertStringMap(Map<String, String> from) {
        HashMap<String, LangSubstitutionValue> result = new HashMap<>();
        for (String key : from.keySet()) {
            result.put(key, new LangSubstitutionValue(from.get(key)));
        }
        return result;
    }

    public String formatTelegramString(String path, Map<String, String> values) {
        return concatTextComponents(formatString("telegram." + path, convertStringMap(values)));
    }

    public ArrayList<TextComponent> formatString(String path, Map<String, LangSubstitutionValue> values) {
        return Objects.requireNonNull(makeSubstitutor(values, path.startsWith("minecraft")).replace(getString(path)));
    }

    public String formatTelegramString(String path, String key, String value) {
        return formatTelegramString(path, Map.of(key, value));
    }


    public String formatMessageMetaString(String path, HashMap<String, String> values) {
        return concatTextComponents(formatString("minecraft.message-meta." + path, convertStringMap(values))) + " ";
    }

    public String formatMessageMetaString(String path, String key, String value) {
        return formatMessageMetaString(path, new HashMap<>(Map.of(key, value)));
    }
}
