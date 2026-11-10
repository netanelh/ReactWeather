package com.natan.vortex.module.client;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** Appends a suffix to your chat messages (hooked in Commands' MODIFY_CHAT listener). */
public class ChatSuffix extends Module {
    public static String suffix = " | Vortex";

    public ChatSuffix() { super("ChatSuffix", "Adds a tag to the end of your chat messages (.suffix <text>)", Category.CLIENT); showInHud = false; }

    public String apply(String msg) { return isEnabled() && !msg.startsWith("/") ? msg + suffix : msg; }
}
