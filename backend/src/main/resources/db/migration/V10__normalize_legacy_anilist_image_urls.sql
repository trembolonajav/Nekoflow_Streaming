UPDATE anime
SET cover_url = 'https://img.anili.st/media/' || substring(cover_url FROM 'bx([0-9]+)')
WHERE cover_url ~ '^https://s4\.anilist\.co/file/anilistcdn/media/anime/cover/large/bx[0-9]+';

UPDATE anime
SET banner_url = 'https://img.anili.st/media/' || substring(banner_url FROM '/banner/([0-9]+)\.')
WHERE banner_url ~ '^https://s4\.anilist\.co/file/anilistcdn/media/anime/banner/[0-9]+\.';
