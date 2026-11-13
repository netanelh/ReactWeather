package dev.netanel.lyricsplus.backend;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt64;

import java.util.Map;
import java.util.List;

/** Spotify on Linux through MPRIS (D-Bus). Lyrics are fetched by LyricsFetcher, not here. */
public class LinuxMusicManager extends MusicManager {

    private static final String PLAYER = "org.mpris.MediaPlayer2.Player";

    private final DBusConnection connection;
    private final Properties properties;

    public LinuxMusicManager() throws Exception {
        connection = DBusConnectionBuilder.forSessionBus().build();
        try {
            properties = connection.getRemoteObject("org.mpris.MediaPlayer2.spotify", "/org/mpris/MediaPlayer2", Properties.class);
            properties.Get(PLAYER, "PlaybackStatus"); // throws if Spotify is not running
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    private Map<String, Object> metadata() {
        Map<String, Object> m = properties.Get(PLAYER, "Metadata");
        return m == null ? Map.of() : m;
    }

    @Override
    public long getPosition() {
        Object position = properties.Get(PLAYER, "Position");
        return position instanceof Number n ? n.longValue() / 1000 : 0;
    }

    @Override
    public boolean isPlaying() {
        Object status = properties.Get(PLAYER, "PlaybackStatus");
        return "Playing".equals(status);
    }

    @Override
    public String getArtist() {
        Object a = metadata().get("xesam:artist");
        if (a instanceof List<?> list && !list.isEmpty() && list.getFirst() != null) {
            return list.getFirst().toString();
        }
        return a instanceof String s ? s : "";
    }

    @Override
    public String getTitle() {
        Object t = metadata().get("xesam:title");
        return t instanceof String s ? s : null;
    }

    @Override
    public String getAlbum() {
        Object al = metadata().get("xesam:album");
        return al instanceof String s ? s : "";
    }

    @Override
    public long getDuration() {
        Object len = metadata().get("mpris:length");
        if (len instanceof UInt64 u) return u.longValue() / 1_000_000;
        if (len instanceof Number n) return n.longValue() / 1_000_000;
        return 0;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }
}
