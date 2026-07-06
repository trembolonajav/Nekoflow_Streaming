package com.nekoflow.backend.api.v1.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nekoflow.backend.api.v1.worker.RssFeedParser.RssItem;

class RssFeedParserTest {

    @Test
    void parsesNyaaStyleItemWithInfoHashElement() {
        String xml = """
            <rss xmlns:nyaa="https://nyaa.si/xmlns/nyaa">
            <channel>
            <item>
              <title>[SubsPlease] Frieren - 01 (1080p) [ABCD1234].mkv</title>
              <link>https://nyaa.si/view/123</link>
              <guid>https://nyaa.si/view/123</guid>
              <nyaa:infoHash>ABCDEF0123456789ABCDEF0123456789ABCDEF01</nyaa:infoHash>
            </item>
            </channel>
            </rss>
            """;

        List<RssItem> items = RssFeedParser.parse(xml);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).title()).contains("Frieren");
        // infohash normalizado para minusculo.
        assertThat(items.get(0).infohash()).isEqualTo("abcdef0123456789abcdef0123456789abcdef01");
        assertThat(items.get(0).guid()).isEqualTo("https://nyaa.si/view/123");
    }

    @Test
    void extractsInfoHashFromMagnetWhenNoNyaaElement() {
        String xml = """
            <rss><channel>
            <item>
              <title><![CDATA[[Erai-raws] Some Anime - 02 [1080p]]]></title>
              <guid>guid-2</guid>
              <link>magnet:?xt=urn:btih:1111111111111111111111111111111111111111&dn=x</link>
            </item>
            </channel></rss>
            """;

        List<RssItem> items = RssFeedParser.parse(xml);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).title()).isEqualTo("[Erai-raws] Some Anime - 02 [1080p]");
        assertThat(items.get(0).infohash()).isEqualTo("1111111111111111111111111111111111111111");
        assertThat(items.get(0).magnet()).startsWith("magnet:?xt=urn:btih:1111");
    }

    @Test
    void skipsItemsWithoutTitle() {
        String xml = """
            <rss><channel>
            <item><guid>x</guid><nyaa:infoHash>2222222222222222222222222222222222222222</nyaa:infoHash></item>
            </channel></rss>
            """;

        assertThat(RssFeedParser.parse(xml)).isEmpty();
    }

    @Test
    void skipsItemsWithoutHashOrMagnet() {
        String xml = """
            <rss><channel>
            <item><title>No hash here</title><guid>x</guid></item>
            </channel></rss>
            """;

        assertThat(RssFeedParser.parse(xml)).isEmpty();
    }

    @Test
    void parsesMultipleItems() {
        String xml = """
            <rss><channel>
            <item><title>A - 01</title><nyaa:infoHash>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</nyaa:infoHash></item>
            <item><title>B - 02</title><link>magnet:?xt=urn:btih:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb</link></item>
            </channel></rss>
            """;

        assertThat(RssFeedParser.parse(xml)).hasSize(2);
    }

    @Test
    void handlesNullAndBlankInput() {
        assertThat(RssFeedParser.parse(null)).isEmpty();
        assertThat(RssFeedParser.parse("")).isEmpty();
        assertThat(RssFeedParser.parse("   ")).isEmpty();
        assertThat(RssFeedParser.parse("<rss></rss>")).isEmpty();
    }
}
