-- A partir daqui o refresh token e guardado apenas como SHA-256 (hex), nunca cru.
-- Um dump do banco deixa de expor sessoes validas.
--
-- Conversao em lugar: o cliente continua com o token cru em maos; no refresh o
-- backend hasheia o valor recebido e compara com o hash armazenado. Assim as
-- sessoes ativas seguem funcionando sem forcar re-login.
--
-- Tokens crus tem 73 chars (uuid.uuid); o hash hex tem 64. O guard evita
-- rehashear caso a migration seja reaplicada sobre dados ja convertidos.
UPDATE refresh_tokens
SET token = encode(sha256(token::bytea), 'hex')
WHERE length(token) <> 64;
