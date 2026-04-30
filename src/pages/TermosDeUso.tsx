import { Footer } from "@/components/layout/Footer";
import { Header } from "@/components/layout/Header";
import { LEGAL_CONTACT_EMAIL, LEGAL_LAST_UPDATED, LEGAL_TERMS_VERSION } from "@/lib/legal";

const sections = [
  {
    title: "1. Identificação da plataforma",
    body:
      "Os presentes Termos de Uso regulam o acesso e a utilização da plataforma Nekoflow, ambiente digital de curadoria, organização e disponibilização de funcionalidades relacionadas à experiência de streaming, catálogo editorial, autenticação de usuários e recursos administrativos.",
  },
  {
    title: "2. Finalidade do serviço",
    body:
      "A plataforma tem por finalidade oferecer ao usuário uma experiência organizada de descoberta, acompanhamento e consumo de conteúdo disponibilizado por meio de integrações técnicas e recursos próprios. A Nekoflow poderá alterar, ampliar, restringir ou descontinuar funcionalidades, total ou parcialmente, a qualquer tempo, conforme critérios operacionais, técnicos, comerciais ou legais.",
  },
  {
    title: "3. Condições de acesso e elegibilidade",
    body:
      "Ao utilizar a plataforma, o usuário declara possuir capacidade civil para praticar atos da vida civil ou estar devidamente assistido ou representado na forma da lei. O usuário também declara que fornecerá dados verídicos, completos e atualizados, sendo responsável por toda atividade realizada por meio de sua conta.",
  },
  {
    title: "4. Conta, credenciais e segurança",
    body:
      "A conta é pessoal, individual e intransferível. O usuário é responsável pela guarda de suas credenciais, pela confidencialidade de sua senha e pelo uso de métodos de autenticação vinculados à conta, inclusive provedores externos, como Google. Qualquer uso não autorizado, suspeita de comprometimento ou acesso indevido deverá ser comunicado imediatamente à plataforma.",
  },
  {
    title: "5. Uso permitido e uso proibido",
    body:
      "É vedado utilizar a plataforma para fins ilícitos, fraudulentos, abusivos ou em desconformidade com estes Termos, incluindo, sem limitação: violação de direitos de terceiros; engenharia reversa indevida; automação maliciosa; tentativa de burlar autenticação, rate limits ou controles de acesso; distribuição de malware; inserção de conteúdo ofensivo, discriminatório ou ilegal; uso comercial não autorizado; scraping ou extração massiva de dados sem permissão expressa.",
  },
  {
    title: "6. Propriedade intelectual",
    body:
      "A estrutura do sistema, sua identidade visual, código, documentação, marcas, organização editorial, textos originais e demais elementos próprios da plataforma são protegidos pela legislação aplicável. O uso da plataforma não transfere ao usuário qualquer titularidade sobre ativos intelectuais da Nekoflow ou de terceiros.",
  },
  {
    title: "7. Conteúdo de terceiros e integrações",
    body:
      "A plataforma pode utilizar integrações, embeds, APIs, provedores de autenticação, metadados, imagens, players e outros recursos de terceiros. A Nekoflow não garante disponibilidade contínua, permanência, compatibilidade, licitude superveniente ou desempenho de serviços externos, os quais poderão sofrer indisponibilidade, limitação, remoção ou alteração sem aviso prévio.",
  },
  {
    title: "8. Disponibilidade e limitação de garantias",
    body:
      "A plataforma é disponibilizada no estado em que se encontra e conforme disponibilidade. A Nekoflow não garante funcionamento ininterrupto, livre de erros, plenamente compatível com todos os ambientes, dispositivos ou navegadores, nem garante que a plataforma atenderá a expectativas particulares do usuário. Atualizações, manutenções, falhas de infraestrutura, incidentes de terceiros e medidas de segurança podem impactar a disponibilidade.",
  },
  {
    title: "9. Limitação de responsabilidade",
    body:
      "Na máxima extensão permitida pela legislação aplicável, a Nekoflow não responderá por danos indiretos, lucros cessantes, perda de chance, perda de receita, perda de dados, indisponibilidade temporária, falhas de serviços de terceiros, atos de usuários, uso indevido da conta, decisões tomadas com base em informações da plataforma ou prejuízos decorrentes de integrações externas. Quando cabível, eventual responsabilidade da plataforma ficará limitada aos limites legalmente admitidos.",
  },
  {
    title: "10. Moderação, suspensão e encerramento de contas",
    body:
      "A Nekoflow poderá, a seu exclusivo critério e sem necessidade de aviso prévio, restringir funcionalidades, suspender sessões, bloquear acessos, remover conteúdo, exigir validações adicionais ou encerrar contas que violem estes Termos, gerem risco operacional, jurídico ou reputacional, ou sejam utilizadas de maneira incompatível com a finalidade da plataforma.",
  },
  {
    title: "11. Alterações destes Termos",
    body:
      "A Nekoflow poderá atualizar estes Termos a qualquer momento. Novas versões passarão a produzir efeitos a partir de sua publicação na plataforma, sem prejuízo da possibilidade de exigir novo aceite quando a alteração for considerada material, relevante ou juridicamente necessária.",
  },
  {
    title: "12. Lei aplicável e foro",
    body:
      "Estes Termos são regidos pelas leis da República Federativa do Brasil. Fica eleito o foro da comarca do domicílio do titular da plataforma, salvo disposição legal imperativa em contrário, para dirimir controvérsias decorrentes destes Termos.",
  },
];

export default function TermosDeUsoPage() {
  return (
    <div className="min-h-screen bg-onyx text-ivory">
      <Header />
      <main className="mx-auto flex w-full max-w-4xl flex-col gap-8 px-6 py-12 md:px-10">
        <header className="border-b border-border-subtle pb-8">
          <p className="font-mono text-[11px] uppercase tracking-[0.24em] text-gold">Termos de Uso</p>
          <h1 className="mt-4 font-serif text-4xl leading-tight">Condições gerais de acesso e utilização da plataforma Nekoflow</h1>
          <p className="mt-4 max-w-2xl text-sm leading-relaxed text-ivory-muted">
            Versão {LEGAL_TERMS_VERSION}. Última atualização em {LEGAL_LAST_UPDATED}. Em caso de dúvidas sobre estes Termos,
            o canal de contato informado pela plataforma é {LEGAL_CONTACT_EMAIL}.
          </p>
        </header>

        <section className="rounded-xl border border-gold/20 bg-surface/60 p-5 text-sm leading-relaxed text-ivory-muted">
          Ao criar uma conta ou utilizar a Nekoflow, o usuário declara ter lido, compreendido e aceitado integralmente estes
          Termos de Uso, bem como a Política de Privacidade aplicável.
        </section>

        <div className="space-y-8">
          {sections.map((section) => (
            <section key={section.title}>
              <h2 className="font-serif text-2xl text-ivory">{section.title}</h2>
              <p className="mt-3 text-sm leading-7 text-ivory-muted">{section.body}</p>
            </section>
          ))}
        </div>
      </main>
      <Footer />
    </div>
  );
}
