import { Link } from "react-router-dom";
import { OrnamentDivider } from "./OrnamentDivider";

/**
 * Footer editorial mínimo, mesma linguagem do header.
 */
export function Footer() {
  const year = new Date().getFullYear();

  return (
    <footer className="mt-12 border-t border-border-subtle bg-onyx/60">
      <div className="mx-auto w-full max-w-[1400px] px-6 py-12 md:px-10">
        <OrnamentDivider width="md" className="mb-10 opacity-60" />

        <div className="grid grid-cols-2 gap-8 md:grid-cols-4">
          <div className="col-span-2 md:col-span-1">
            <span className="font-serif text-2xl font-medium tracking-tight text-ivory">
              Nekoflow
            </span>
            <p className="mt-3 max-w-xs text-xs leading-relaxed text-ivory-muted">
              Curadoria editorial para o melhor do anime — temporada atual,
              clássicos e joias de autor.
            </p>
          </div>

          <FooterColumn
            title="Navegação"
            items={[
              { label: "Início", to: "/" },
              { label: "Calendário", to: "/" },
              { label: "Explorar", to: "/" },
            ]}
          />
          <FooterColumn
            title="Conta"
            items={[
              { label: "Minha lista", to: "/" },
              { label: "Configurações", to: "/" },
              { label: "Notificações", to: "/" },
            ]}
          />
          <FooterColumn
            title="Sobre"
            items={[
              { label: "Curadoria", to: "/" },
              { label: "Contato", to: "/" },
              { label: "Termos", to: "/" },
            ]}
          />
        </div>

        <div className="mt-12 flex flex-col items-center justify-between gap-3 border-t border-border-subtle pt-6 sm:flex-row">
          <span className="text-[10px] uppercase tracking-[0.22em] text-ivory-muted">
            © {year} Nekoflow · Feito com cuidado
          </span>
          <span className="text-[10px] uppercase tracking-[0.22em] text-gold/70">
            Sua próxima obsessão
          </span>
        </div>
      </div>
    </footer>
  );
}

function FooterColumn({
  title,
  items,
}: {
  title: string;
  items: { label: string; to: string }[];
}) {
  return (
    <div className="flex flex-col gap-3">
      <span className="text-[10px] uppercase tracking-[0.28em] text-gold">
        {title}
      </span>
      <ul className="flex flex-col gap-2">
        {items.map((i) => (
          <li key={i.label}>
            <Link
              to={i.to}
              className="text-sm text-ivory-muted transition-colors hover:text-gold"
            >
              {i.label}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
