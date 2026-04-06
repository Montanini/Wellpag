"use client";

import { useEffect, useState } from "react";
import { Navbar } from "@/components/Navbar";
import { api } from "@/lib/api";
import { Aluno, Mensalidade } from "@/lib/types";
import {
  NotificacaoResponse,
  StatusNotificacao,
  WebhookConfiguracaoResponse,
} from "@/lib/notificacao-types";

function formatBRL(valor?: number) {
  if (valor == null) return "—";
  return valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function formatData(iso?: string) {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

const STATUS_CONFIG: Record<StatusNotificacao, { label: string; className: string }> = {
  PENDENTE:  { label: "Pendente",  className: "bg-yellow-100 text-yellow-800 border border-yellow-200" },
  VINCULADA: { label: "Vinculada", className: "bg-green-100 text-green-800 border border-green-200"   },
  IGNORADA:  { label: "Ignorada",  className: "bg-gray-100 text-gray-500 border border-gray-200"      },
};

export default function NotificacoesPage() {
  const [notificacoes, setNotificacoes] = useState<NotificacaoResponse[]>([]);
  const [config, setConfig] = useState<WebhookConfiguracaoResponse | null>(null);
  const [filtro, setFiltro] = useState<StatusNotificacao | "TODAS">("PENDENTE");
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState<NotificacaoResponse | null>(null);
  const [configAberta, setConfigAberta] = useState(false);

  useEffect(() => {
    carregar();
    api.get<WebhookConfiguracaoResponse>("/professor/notificacoes/webhook-configuracao")
      .then(setConfig)
      .catch(() => {});
  }, []);

  async function carregar() {
    setLoading(true);
    const query = filtro !== "TODAS" ? `?status=${filtro}` : "";
    const data = await api.get<NotificacaoResponse[]>(`/professor/notificacoes${query}`);
    setNotificacoes(data);
    setLoading(false);
  }

  useEffect(() => { carregar(); }, [filtro]);

  async function ignorar(id: string) {
    const atualizada = await api.patch<NotificacaoResponse>(`/professor/notificacoes/${id}/ignorar`, {});
    setNotificacoes((prev) => prev.map((n) => n.id === id ? atualizada : n));
  }

  const pendentes = notificacoes.filter((n) => n.status === "PENDENTE").length;

  return (
    <div className="min-h-screen">
      <Navbar />

      <main className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">Notificações de Pagamento</h1>
            {pendentes > 0 && (
              <p className="text-sm text-yellow-600 mt-0.5">
                {pendentes} notificaç{pendentes === 1 ? "ão" : "ões"} pendente{pendentes !== 1 ? "s" : ""}
              </p>
            )}
          </div>
          <button
            onClick={() => setConfigAberta(true)}
            className="text-sm text-brand-600 border border-brand-200 rounded-lg px-4 py-2 hover:bg-brand-50 transition-colors"
          >
            Configurar Banco
          </button>
        </div>

        {/* Filtro */}
        <div className="flex gap-2 flex-wrap">
          {(["PENDENTE", "VINCULADA", "IGNORADA", "TODAS"] as const).map((s) => (
            <button
              key={s}
              onClick={() => setFiltro(s)}
              className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
                filtro === s
                  ? "bg-gray-800 text-white"
                  : "bg-white border border-gray-200 text-gray-600 hover:bg-gray-50"
              }`}
            >
              {s === "TODAS" ? "Todas" : STATUS_CONFIG[s].label}
            </button>
          ))}
        </div>

        {/* Lista */}
        {loading ? (
          <div className="text-center py-16 text-gray-400 text-sm">Carregando...</div>
        ) : notificacoes.length === 0 ? (
          <div className="text-center py-16 text-gray-400 text-sm">
            Nenhuma notificação {filtro !== "TODAS" ? `com status "${STATUS_CONFIG[filtro as StatusNotificacao]?.label}"` : ""}.
          </div>
        ) : (
          <div className="space-y-3">
            {notificacoes.map((n) => (
              <div
                key={n.id}
                className="bg-white rounded-xl border border-gray-200 p-4 hover:border-gray-300 transition-colors"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_CONFIG[n.status].className}`}>
                        {STATUS_CONFIG[n.status].label}
                      </span>
                      <span className="text-xs text-gray-400">{n.banco.replace("_", " ")}</span>
                      <span className="text-xs text-gray-300">·</span>
                      <span className="text-xs text-gray-400">{formatData(n.recebidaEm)}</span>
                    </div>

                    <div className="mt-2 flex flex-wrap gap-4 text-sm">
                      <div>
                        <span className="text-gray-400 text-xs">Valor</span>
                        <p className="font-bold text-green-600">{formatBRL(n.valor)}</p>
                      </div>
                      {n.nomePagador && (
                        <div>
                          <span className="text-gray-400 text-xs">Pagador</span>
                          <p className="font-medium">{n.nomePagador}</p>
                        </div>
                      )}
                      {n.dataTransacao && (
                        <div>
                          <span className="text-gray-400 text-xs">Data transação</span>
                          <p>{formatData(n.dataTransacao)}</p>
                        </div>
                      )}
                      {n.descricao && (
                        <div>
                          <span className="text-gray-400 text-xs">Descrição</span>
                          <p className="text-gray-600">{n.descricao}</p>
                        </div>
                      )}
                    </div>
                  </div>

                  {n.status === "PENDENTE" && (
                    <div className="flex flex-col gap-2 shrink-0">
                      <button
                        onClick={() => setModal(n)}
                        className="bg-brand-600 hover:bg-brand-700 text-white text-xs font-medium px-3 py-1.5 rounded-lg transition-colors"
                      >
                        Vincular
                      </button>
                      <button
                        onClick={() => ignorar(n.id)}
                        className="text-gray-400 hover:text-gray-600 text-xs transition-colors"
                      >
                        Ignorar
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Modal: vincular notificação */}
      {modal && (
        <VincularModal
          notificacao={modal}
          onClose={() => setModal(null)}
          onVinculada={(atualizada) => {
            setNotificacoes((prev) => prev.map((n) => n.id === atualizada.id ? atualizada : n));
            setModal(null);
          }}
        />
      )}

      {/* Modal: configuração de webhook */}
      {configAberta && config && (
        <ConfigModal config={config} onClose={() => setConfigAberta(false)} />
      )}
    </div>
  );
}

// ---

function VincularModal({
  notificacao,
  onClose,
  onVinculada,
}: {
  notificacao: NotificacaoResponse;
  onClose: () => void;
  onVinculada: (n: NotificacaoResponse) => void;
}) {
  const [alunos, setAlunos] = useState<Aluno[]>([]);
  const [alunoSelecionado, setAlunoSelecionado] = useState("");
  const [mensalidades, setMensalidades] = useState<Mensalidade[]>([]);
  const [mensalidadeSelecionada, setMensalidadeSelecionada] = useState("");
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    api.get<Aluno[]>("/professor/alunos").then(setAlunos);
  }, []);

  useEffect(() => {
    if (!alunoSelecionado) { setMensalidades([]); return; }
    api.get<Mensalidade[]>(`/professor/mensalidades/aluno/${alunoSelecionado}`).then((lista) => {
      setMensalidades(lista.filter((m) => m.status !== "PAGO"));
    });
  }, [alunoSelecionado]);

  async function salvar() {
    if (!alunoSelecionado || !mensalidadeSelecionada) return;
    setSalvando(true);
    try {
      const atualizada = await api.patch<NotificacaoResponse>(
        `/professor/notificacoes/${notificacao.id}/vincular`,
        { alunoId: alunoSelecionado, mensalidadeId: mensalidadeSelecionada }
      );
      onVinculada(atualizada);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 px-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
        <h2 className="font-semibold text-lg mb-1">Vincular pagamento</h2>
        <p className="text-sm text-gray-500 mb-4">
          Selecione o aluno e a mensalidade correspondente a este pagamento de{" "}
          <strong className="text-green-600">
            {notificacao.valor?.toLocaleString("pt-BR", { style: "currency", currency: "BRL" })}
          </strong>.
        </p>

        <div className="space-y-3">
          <div>
            <label className="block text-xs text-gray-500 mb-1">Aluno</label>
            <select
              value={alunoSelecionado}
              onChange={(e) => { setAlunoSelecionado(e.target.value); setMensalidadeSelecionada(""); }}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="">Selecione o aluno...</option>
              {alunos.map((a) => <option key={a.id} value={a.id}>{a.nome}</option>)}
            </select>
          </div>

          {mensalidades.length > 0 && (
            <div>
              <label className="block text-xs text-gray-500 mb-1">Mensalidade</label>
              <select
                value={mensalidadeSelecionada}
                onChange={(e) => setMensalidadeSelecionada(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
              >
                <option value="">Selecione a mensalidade...</option>
                {mensalidades.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.mesReferencia} · R$ {m.valor?.toFixed(2)} · {m.status}
                  </option>
                ))}
              </select>
            </div>
          )}

          {alunoSelecionado && mensalidades.length === 0 && (
            <p className="text-xs text-gray-400">Nenhuma mensalidade em aberto para este aluno.</p>
          )}
        </div>

        <div className="flex gap-3 mt-6">
          <button onClick={onClose} className="flex-1 border border-gray-200 text-gray-600 text-sm py-2 rounded-lg hover:bg-gray-50 transition-colors">
            Cancelar
          </button>
          <button
            onClick={salvar}
            disabled={!alunoSelecionado || !mensalidadeSelecionada || salvando}
            className="flex-1 bg-brand-600 hover:bg-brand-700 disabled:opacity-50 text-white text-sm font-medium py-2 rounded-lg transition-colors"
          >
            {salvando ? "Salvando..." : "Confirmar pagamento"}
          </button>
        </div>
      </div>
    </div>
  );
}

function ConfigModal({
  config,
  onClose,
}: {
  config: WebhookConfiguracaoResponse;
  onClose: () => void;
}) {
  async function copiar(url: string) {
    await navigator.clipboard.writeText(url);
    alert("URL copiada!");
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 px-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg p-6">
        <h2 className="font-semibold text-lg mb-1">Configurar Webhook Bancário</h2>
        <p className="text-sm text-gray-500 mb-4">
          Configure a URL correspondente ao seu banco nas configurações de webhook/PIX da sua conta.
        </p>

        <div className="space-y-3">
          {Object.entries(config.urls).map(([banco, url]) => (
            <div key={banco} className="border border-gray-100 rounded-lg p-3">
              <p className="text-xs font-medium text-gray-600 mb-1">{banco}</p>
              <div className="flex items-center gap-2">
                <code className="flex-1 text-xs bg-gray-50 rounded px-2 py-1 text-gray-700 truncate">{url}</code>
                <button
                  onClick={() => copiar(url)}
                  className="text-xs text-brand-600 hover:underline shrink-0"
                >
                  Copiar
                </button>
              </div>
            </div>
          ))}
        </div>

        <p className="text-xs text-gray-400 mt-4">
          Seu token: <code className="bg-gray-50 px-1 rounded">{config.token}</code>
        </p>

        <button
          onClick={onClose}
          className="mt-4 w-full border border-gray-200 text-gray-600 text-sm py-2 rounded-lg hover:bg-gray-50 transition-colors"
        >
          Fechar
        </button>
      </div>
    </div>
  );
}
