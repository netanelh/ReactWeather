package com.natan.vortex.module.client;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** While on, combat modules never target players on your friends list (.friend add/del/list). */
public class FriendsModule extends Module {
    public FriendsModule() { super("Friends", "Protects friends from combat modules (.friend add <name>)", Category.CLIENT); showInHud = false; setEnabled(true); }
}
