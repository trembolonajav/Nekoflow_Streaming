import { Footer } from "@/components/layout/Footer";
import { Header } from "@/components/layout/Header";
import { LEGAL_CONTACT_EMAIL, LEGAL_LAST_UPDATED, LEGAL_PRIVACY_VERSION } from "@/lib/legal";

const sections = [
  {
    title: "1. Escopo desta Política",
    body:
      "Esta Política de Privacidade descreve como a Nekoflow coleta, utiliza, armazena, compartilha e protege dados pessoais no contexto do uso da plataforma, observando, em especial, os princípios e diretrizes da Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018 - LGPD).",
  },
  {
    title: "2. Dados coletados diretamente do usuário",
    body:
      "Podemos coletar dados fornecidos diretamente pelo usuário, tais como nome, endereço de e-mail, senha criptograficamente protegida, preferências de conta, histórico de interações, configurações pessoais e informações eventualmente encaminhadas por formulários, comentários, solicitações de suporte ou fluxos de autenticação.",
  },
  {
    title: "3. Dados coletados automaticamente",
    body:
      "A plataforma poderá coletar dados técnicos e de navegação, incluindo endereço IP, user-agent, identificadores de sessão, registros de autenticação, data e hora de acesso, páginas visitadas, versão de documentos aceitos, informações básicas de dispositivo, navegador e logs necessários para segurança, auditoria, prevenção a fraudes, estabilidade e melhoria do serviço.",
  },
  {
    title: "4. Dados recebidos por integração com Google",
    body:
      "Quando o usuário optar por autenticação via Google, a plataforma poderá receber dados como identificador único da conta no Google, nome, endereço de e-mail e imagem de perfil, estritamente nos limites informados pelo provedor e necessários ao fluxo de autenticação e vinculação de conta.",
  },
  {
    title: "5. Finalidades do tratamento",
    body:
      "Os dados pessoais poderão ser tratados para: autenticação e gestão de contas; prestação e personalização do serviço; manutenção de sessão; prevenção a fraude e abuso; registro de aceite de documentos jurídicos; atendimento a solicitações; melhoria contínua da plataforma; análise de desempenho; cumprimento de obrigações legais, regulatórias ou judiciais; e exercício regular de direitos em processos administrativos, arbitrais ou judiciais.",
  },
  {
    title: "6. Bases legais",
    body:
      "O tratamento poderá se apoiar, conforme o caso concreto, nas bases legais previstas na LGPD, incluindo execução de contrato ou de procedimentos preliminares relacionados ao contrato, cumprimento de obrigação legal ou regulatória, exercício regular de direitos, legítimo interesse e consentimento, quando exigido ou apropriado.",
  },
  {
    title: "7. Compartilhamento de dados",
    body:
      "A Nekoflow poderá compartilhar dados com provedores de infraestrutura, autenticação, hospedagem, segurança, análise, mensageria, suporte e integrações técnicas estritamente necessários ao funcionamento da plataforma, além de autoridades públicas e terceiros quando houver dever legal, ordem judicial, requisição válida ou necessidade de resguardar direitos e segurança da plataforma.",
  },
  {
    title: "8. Armazenamento, retenção e segurança",
    body:
      "Adotamos medidas técnicas e organizacionais razoáveis para proteção de dados pessoais contra acessos não autorizados, perda, destruição, alteração ou divulgação indevida. Os dados serão armazenados pelo prazo necessário ao cumprimento das finalidades descritas nesta Política, observadas exigências legais, regulatórias, contratuais, probatórias e de segurança. Nenhuma medida, contudo, é absoluta ou imune a riscos.",
  },
  {
    title: "9. Direitos do titular",
    body:
      "Nos termos da LGPD, o titular poderá solicitar confirmação da existência de tratamento, acesso, correção, anonimização, bloqueio, eliminação, portabilidade, informação sobre compartilhamento, revogação de consentimento e outros direitos aplicáveis, observados limites legais e operacionais. A plataforma poderá solicitar informações adicionais para validar a identidade do requerente antes de cumprir solicitações.",
  },
  {
    title: "10. Cookies e tecnologias semelhantes",
    body:
      "A plataforma poderá utilizar cookies, armazenamento local, tokens de sessão e tecnologias similares para autenticação, segurança, estabilidade e personalização mínima da experiência. Caso novas categorias de cookies não estritamente necessários sejam adotadas, a plataforma poderá ajustar seus avisos e controles conforme a necessidade.",
  },
  {
    title: "11. Alterações desta Política",
    body:
      "Esta Política poderá ser atualizada periodicamente para refletir alterações legais, regulatórias, operacionais ou técnicas. Em caso de mudanças materiais, a plataforma poderá destacar a nova versão e, quando necessário, solicitar novo aceite.",
  },
  {
    title: "12. Canal de contato",
    body:
      `Solicitações relacionadas à privacidade, proteção de dados e exercício de direitos do titular podem ser encaminhadas ao canal oficial informado pela plataforma: ${LEGAL_CONTACT_EMAIL}.`,
  },
];

export default function PoliticaPrivacidadePage() {
  return (
    <div className="min-h-screen bg-onyx text-ivory">
      <Header />
      <main className="mx-auto flex w-full max-w-4xl flex-col gap-8 px-6 py-12 md:px-10">
        <header className="border-b border-border-subtle pb-8">
          <p className="font-mono text-[11px] uppercase tracking-[0.24em] text-gold">Política de Privacidade</p>
          <h1 className="mt-4 font-serif text-4xl leading-tight">Tratamento de dados pessoais na plataforma Nekoflow</h1>
          <p className="mt-4 max-w-2xl text-sm leading-relaxed text-ivory-muted">
            Versão {LEGAL_PRIVACY_VERSION}. Última atualização em {LEGAL_LAST_UPDATED}. Este documento complementa os
            Termos de Uso e deve ser lido em conjunto com eles.
          </p>
        </header>

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
