package com.nekoflow.backend.api.v1.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser de feed RSS de releases (nyaa, erai-raws, subsplease, etc.).
 *
 * Classe pura (sem HTTP/estado) para ser testavel sem rede. Extrai de cada
 * &lt;item&gt;: titulo, guid, infohash e magnet. O infohash vem de
 * &lt;nyaa:infoHash&gt; quando existe, senao do proprio magnet.
 */
public final class RssFeedParser {

    public record RssItem(String title, String guid, String infohash, String magnet) {
    }

    private static final Pattern ITEM = Pattern.compile("<item[\\s>].*?</item>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern GUID = Pattern.compile("<guid[^>]*>(.*?)</guid>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern NYAA_HASH = Pattern.compile("<nyaa:infoHash>(.*?)</nyaa:infoHash>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern MAGNET = Pattern.compile("magnet:\\?[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern BTIH = Pattern.compile("xt=urn:btih:([0-9a-fA-F]{40}|[a-zA-Z2-7]{32})", Pattern.CASE_INSENSITIVE);

    private RssFeedParser() {
    }

    public static List<RssItem> parse(String xml) {
        List<RssItem> items = new ArrayList<>();
        if (xml == null || xml.isBlank()) {
            return items;
        }
        Matcher itemMatcher = ITEM.matcher(xml);
        while (itemMatcher.find()) {
            String block = itemMatcher.group();
            String title = clean(first(TITLE, block));
            String magnet = first(MAGNET, block);
            String infohash = normalizeHash(first(NYAA_HASH, block));
            if (infohash == null && magnet != null) {
                Matcher hashInMagnet = BTIH.matcher(magnet);
                if (hashInMagnet.find()) {
                    infohash = normalizeHash(hashInMagnet.group(1));
                }
            }
            if (title == null || (infohash == null && magnet == null)) {
                continue;
            }
            items.add(new RssItem(title, clean(first(GUID, block)), infohash, magnet));
        }
        return items;
    }

    private static String first(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1 <= matcher.groupCount() ? 1 : 0) : null;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String result = value
            .replaceAll("<!\\[CDATA\\[", "")
            .replaceAll("]]>", "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .trim();
        return result.isBlank() ? null : result;
    }

    private static String normalizeHash(String value) {
        if (value == null) {
            return null;
        }
        String hash = value.trim().toLowerCase(Locale.ROOT);
        return hash.isBlank() ? null : hash;
    }
}
