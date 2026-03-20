/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.jsf.userprofile;

import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

/**
 * Provides color palette data for 8 theme colors in both light and dark mode.
 * Used by template.xhtml to set CSS custom properties.
 */
@Component("themeColorProvider")
@Scope(value = "application", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ThemeColorProvider implements Serializable {

    @Getter
    public static class ColorPalette implements Serializable {
        private final String primary;
        private final String primaryText;
        private final String primaryLighter;
        private final String primaryBg16;
        private final String primaryBg04;
        private final String focusRing;
        // Dark mode variants
        private final String primaryDark;
        private final String primaryLighterDark;
        private final String primaryBg16Dark;
        private final String primaryBg04Dark;
        private final String focusRingDark;

        public ColorPalette(String primary, String primaryText,
                            String primaryLighter, String primaryBg16, String primaryBg04, String focusRing,
                            String primaryDark, String primaryLighterDark,
                            String primaryBg16Dark, String primaryBg04Dark, String focusRingDark) {
            this.primary = primary;
            this.primaryText = primaryText;
            this.primaryLighter = primaryLighter;
            this.primaryBg16 = primaryBg16;
            this.primaryBg04 = primaryBg04;
            this.focusRing = focusRing;
            this.primaryDark = primaryDark;
            this.primaryLighterDark = primaryLighterDark;
            this.primaryBg16Dark = primaryBg16Dark;
            this.primaryBg04Dark = primaryBg04Dark;
            this.focusRingDark = focusRingDark;
        }
    }

    private static final Map<String, ColorPalette> PALETTES = Map.of(
            "blue", new ColorPalette(
                    "#2196F3", "#ffffff", "#e3f2fd",
                    "rgba(33,150,243,.16)", "rgba(33,150,243,.04)", "rgba(33,150,243,.5)",
                    "#69B7FF", "#1e3a5f",
                    "rgba(105,183,255,.16)", "rgba(105,183,255,.04)", "rgba(105,183,255,.5)"),
            "green", new ColorPalette(
                    "#4CAF50", "#ffffff", "#e8f5e9",
                    "rgba(76,175,80,.16)", "rgba(76,175,80,.04)", "rgba(76,175,80,.5)",
                    "#81C784", "#1b3d1c",
                    "rgba(129,199,132,.16)", "rgba(129,199,132,.04)", "rgba(129,199,132,.5)"),
            "orange", new ColorPalette(
                    "#FF9800", "#ffffff", "#fff3e0",
                    "rgba(255,152,0,.16)", "rgba(255,152,0,.04)", "rgba(255,152,0,.5)",
                    "#FFB74D", "#3e2700",
                    "rgba(255,183,77,.16)", "rgba(255,183,77,.04)", "rgba(255,183,77,.5)"),
            "turquoise", new ColorPalette(
                    "#00BCD4", "#ffffff", "#e0f7fa",
                    "rgba(0,188,212,.16)", "rgba(0,188,212,.04)", "rgba(0,188,212,.5)",
                    "#4DD0E1", "#003d44",
                    "rgba(77,208,225,.16)", "rgba(77,208,225,.04)", "rgba(77,208,225,.5)"),
            "avocado", new ColorPalette(
                    "#AEC523", "#ffffff", "#f4f7e0",
                    "rgba(174,197,35,.16)", "rgba(174,197,35,.04)", "rgba(174,197,35,.5)",
                    "#C6D63E", "#2d3308",
                    "rgba(198,214,62,.16)", "rgba(198,214,62,.04)", "rgba(198,214,62,.5)"),
            "purple", new ColorPalette(
                    "#7B1FA2", "#ffffff", "#f3e5f5",
                    "rgba(123,31,162,.16)", "rgba(123,31,162,.04)", "rgba(123,31,162,.5)",
                    "#BA68C8", "#2a0936",
                    "rgba(186,104,200,.16)", "rgba(186,104,200,.04)", "rgba(186,104,200,.5)"),
            "red", new ColorPalette(
                    "#F44336", "#ffffff", "#ffebee",
                    "rgba(244,67,54,.16)", "rgba(244,67,54,.04)", "rgba(244,67,54,.5)",
                    "#E57373", "#3e0d0a",
                    "rgba(229,115,115,.16)", "rgba(229,115,115,.04)", "rgba(229,115,115,.5)"),
            "yellow", new ColorPalette(
                    "#FFC107", "#212529", "#fff8e1",
                    "rgba(255,193,7,.16)", "rgba(255,193,7,.04)", "rgba(255,193,7,.5)",
                    "#FFD54F", "#3e3000",
                    "rgba(255,213,79,.16)", "rgba(255,213,79,.04)", "rgba(255,213,79,.5)")
    );

    public ColorPalette getPalette(String colorName) {
        return PALETTES.getOrDefault(colorName, PALETTES.get("blue"));
    }

    /**
     * Get the primary color for a given theme and mode.
     */
    public String getPrimaryColor(String colorName, String darkMode) {
        ColorPalette p = getPalette(colorName);
        return "dark".equals(darkMode) ? p.getPrimaryDark() : p.getPrimary();
    }

    public String getPrimaryColorText(String colorName) {
        return getPalette(colorName).getPrimaryText();
    }

    public String getPrimaryLighter(String colorName, String darkMode) {
        ColorPalette p = getPalette(colorName);
        return "dark".equals(darkMode) ? p.getPrimaryLighterDark() : p.getPrimaryLighter();
    }

    public String getPrimaryBg16(String colorName, String darkMode) {
        ColorPalette p = getPalette(colorName);
        return "dark".equals(darkMode) ? p.getPrimaryBg16Dark() : p.getPrimaryBg16();
    }

    public String getPrimaryBg04(String colorName, String darkMode) {
        ColorPalette p = getPalette(colorName);
        return "dark".equals(darkMode) ? p.getPrimaryBg04Dark() : p.getPrimaryBg04();
    }

    public String getFocusRing(String colorName, String darkMode) {
        ColorPalette p = getPalette(colorName);
        return "dark".equals(darkMode) ? p.getFocusRingDark() : p.getFocusRing();
    }

    /**
     * Returns a JSON map of all color palettes for client-side JavaScript.
     */
    public String getColorsJson() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : PALETTES.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            ColorPalette p = entry.getValue();
            sb.append("'").append(entry.getKey()).append("':{")
                    .append("light:{")
                    .append("primary:'").append(p.getPrimary()).append("',")
                    .append("primaryText:'").append(p.getPrimaryText()).append("',")
                    .append("primaryLighter:'").append(p.getPrimaryLighter()).append("',")
                    .append("primaryBg16:'").append(p.getPrimaryBg16()).append("',")
                    .append("primaryBg04:'").append(p.getPrimaryBg04()).append("',")
                    .append("focusRing:'").append(p.getFocusRing()).append("'")
                    .append("},dark:{")
                    .append("primary:'").append(p.getPrimaryDark()).append("',")
                    .append("primaryText:'").append(p.getPrimaryText()).append("',")
                    .append("primaryLighter:'").append(p.getPrimaryLighterDark()).append("',")
                    .append("primaryBg16:'").append(p.getPrimaryBg16Dark()).append("',")
                    .append("primaryBg04:'").append(p.getPrimaryBg04Dark()).append("',")
                    .append("focusRing:'").append(p.getFocusRingDark()).append("'")
                    .append("}}");
        }
        sb.append("}");
        return sb.toString();
    }
}
