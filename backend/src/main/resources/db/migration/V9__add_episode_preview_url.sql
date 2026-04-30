ALTER TABLE episodes
ADD COLUMN IF NOT EXISTS preview_url TEXT;

UPDATE episodes
SET preview_url = 'https://nekoflow.embedseek.com/_OxHIpFY5N2Yt1SaU9kZ0Q/miy/xieomuss/tgrdfw/preview.webp'
WHERE number = 2
  AND anime_id IN (
    SELECT id
    FROM anime
    WHERE slug = 'dorohedoro'
  )
  AND (preview_url IS NULL OR preview_url = '');
