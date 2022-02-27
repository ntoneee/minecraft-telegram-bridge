package one.ntonee.mctgbridge;

import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;

public class LangSubstitutionValue {
    ArrayList<TextComponent> components;

    LangSubstitutionValue(String string) {
        components = new ArrayList<>();
        components.add(new TextComponent(string));
    }

    LangSubstitutionValue(ArrayList<TextComponent> components) {
        this.components = components;
    }

    public ArrayList<TextComponent> getComponents() {
        return components;
    }
}
