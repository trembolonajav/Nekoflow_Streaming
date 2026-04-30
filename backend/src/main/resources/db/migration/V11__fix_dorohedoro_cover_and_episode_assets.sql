UPDATE anime
SET cover_url = 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx105228-I4xr84QS9Pvk.jpg'
WHERE slug = 'dorohedoro';

UPDATE episodes
SET thumbnail_url = 'https://asset.seekstreaming.info/cik3K5L8QcoV1OsG4ftuqw/miy/xieomuss/tgrdfw/poster.png',
    preview_url = 'https://asset.seekstreaming.info/_OxHIpFY5N2Yt1SaU9kZ0Q/miy/xieomuss/tgrdfw/preview.webp'
WHERE anime_id = (SELECT id FROM anime WHERE slug = 'dorohedoro')
  AND number = 2;
