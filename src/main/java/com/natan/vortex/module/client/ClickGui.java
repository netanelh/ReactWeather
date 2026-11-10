package com.natan.vortex.module.client;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Colors;
import org.lwjgl.glfw.GLFW;

/** Not a toggle: its keybind opens the GUI and its settings style the GUI. */
public class ClickGui extends Module {
    public final ModeSetting accent = add(new ModeSetting("Accent", "Main GUI/HUD color", "Purple", Colors.NAMES));
    public final NumberSetting dim = add(new NumberSetting("Background", "How dark the screen behind the GUI gets (%)", 45, 0, 90, 5));
    public final BoolSetting descriptions = add(new BoolSetting("Descriptions", "Show a description bar for what you hover", true));
    public final NumberSetting panelWidth = add(new NumberSetting("Panel Width", "Width of each category panel", 112, 90, 160, 2));
    public final BoolSetting resetLayout = add(new BoolSetting("Reset Layout", "Click to re-arrange the panels neatly", false));

    public ClickGui() {
        super("ClickGUI", "Open with Right Shift (rebind here)", Category.CLIENT);
        key = GLFW.GLFW_KEY_RIGHT_SHIFT;
        showInHud = false;
    }

    public int accent(int offset) { return Colors.named(accent.get(), offset); }

    @Override public void setEnabled(boolean value) { /* never stays on */ }
}
