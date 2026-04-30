INSERT INTO roles (code, description)
VALUES
    ('ADMIN', 'Administrador do sistema'),
    ('EDITOR', 'Editor de catalogo e home'),
    ('MODERATOR', 'Moderador da comunidade'),
    ('USER', 'Usuario padrao')
ON CONFLICT (code) DO NOTHING;

INSERT INTO home_sections (code, title, mode, is_active, sort_order)
VALUES
    ('hero', 'Hero principal', 'MANUAL', TRUE, 1),
    ('continue', 'Continuar assistindo', 'AUTOMATIC', TRUE, 2),
    ('season', 'Novidades da temporada', 'HYBRID', TRUE, 3),
    ('recent', 'Episodios recentes', 'AUTOMATIC', TRUE, 4)
ON CONFLICT (code) DO NOTHING;
