package dev.netanel.lyricsplus.backend;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses LRC text: several time tags per line, [offset:N], and enhanced &lt;mm:ss.xx&gt; word tags. */
public class LyricsManager {

    private static final Pattern TIME_TAG = Pattern.compile("^\\[(\\d+):(\\d+)(?:[.:](\\d{1,3}))?\\]");
    private static final Pattern OFFSET_TAG = Pattern.compile("^\\[offset:\\s*([+-]?\\d+)\\s*\\]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORD_TAG = Pattern.compile("<\\d+:\\d+(?:[.:]\\d+)?>");

    public List<LyricsLine> parseLyrics(String syncedLyrics) {
        List<LyricsLine> lyrics = new ArrayList<>();
        if (syncedLyrics == null || syncedLyrics.isBlank()) {
            return lyrics;
        }

        long offset = 0;
        for (String raw : syncedLyrics.split("\\R")) {
            Matcher off = OFFSET_TAG.matcher(raw.trim());
            if (off.matches()) {
                try {
                    offset = Long.parseLong(off.group(1));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        for (String raw : syncedLyrics.split("\\R")) {
            String line = raw.trim();
            List<Long> stamps = new ArrayList<>(2);
            Matcher m = TIME_TAG.matcher(line);
            try {
                while (m.find()) {
                    long minutes = Long.parseLong(m.group(1));
                    long seconds = Long.parseLong(m.group(2));
                    long ms = 0;
                    String frac = m.group(3);
                    if (frac != null) {
                        ms = switch (frac.length()) {
                            case 1 -> Long.parseLong(frac) * 100;
                            case 2 -> Long.parseLong(frac) * 10;
                            default -> Long.parseLong(frac);
                        };
                    }
                    stamps.add((minutes * 60 + seconds) * 1000 + ms);
                    line = line.substring(m.end()).trim();
                    m = TIME_TAG.matcher(line);
                }
            } catch (NumberFormatException e) {
                continue; // invalid timestamp: skip the line
            }
            if (stamps.isEmpty()) {
                continue; // metadata like [ar:..] or plain text
            }
            String text = WORD_TAG.matcher(line).replaceAll("").replaceAll("\\s{2,}", " ").trim();
            for (long ts : stamps) {
                // positive offset = lyrics come earlier
                lyrics.add(new LyricsLine(Math.max(0, ts - offset), text));
            }
        }

        lyrics.sort(Comparator.comparingLong(LyricsLine::getTimeStamp)); // stable: equal times keep file order
        return lyrics;
    }
}
