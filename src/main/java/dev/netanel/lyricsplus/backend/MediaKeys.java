package dev.netanel.lyricsplus.backend;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Play/pause, next and previous for whatever app is playing, by pressing the system
 * media keys (Windows: user32 keybd_event; Linux: playerctl).
 */
public final class MediaKeys {

    private static final int VK_MEDIA_NEXT_TRACK = 0xB0;
    private static final int VK_MEDIA_PREV_TRACK = 0xB1;
    private static final int VK_MEDIA_PLAY_PAUSE = 0xB3;
    private static final int KEYEVENTF_KEYUP = 0x0002;

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");
    private static MethodHandle keybdEvent;

    private MediaKeys() {
    }

    public static void playPause() {
        send(VK_MEDIA_PLAY_PAUSE, "play-pause");
    }

    public static void next() {
        send(VK_MEDIA_NEXT_TRACK, "next");
    }

    public static void previous() {
        send(VK_MEDIA_PREV_TRACK, "previous");
    }

    private static void send(int vk, String playerctlCommand) {
        try {
            if (WINDOWS) {
                if (keybdEvent == null) {
                    SymbolLookup user32 = SymbolLookup.libraryLookup("user32.dll", Arena.global());
                    keybdEvent = Linker.nativeLinker().downcallHandle(
                            user32.find("keybd_event").orElseThrow(),
                            FunctionDescriptor.ofVoid(ValueLayout.JAVA_BYTE, ValueLayout.JAVA_BYTE,
                                    ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
                }
                keybdEvent.invoke((byte) vk, (byte) 0, 0, 0L);
                keybdEvent.invoke((byte) vk, (byte) 0, KEYEVENTF_KEYUP, 0L);
            } else {
                new ProcessBuilder("playerctl", playerctlCommand).start();
            }
        } catch (Throwable e) {
            System.err.println("[Lyrics Plus] media key failed: " + e.getMessage());
        }
    }
}
