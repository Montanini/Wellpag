"use client";

import { useEffect, useRef, useState } from "react";
import { Navbar } from "@/components/Navbar";
import { api } from "@/lib/api";

interface ConexaoResponse {
  instanceName: string | null;
  estado: "open" | "connecting" | "close" | "desconectado";
  qrCodeBase64: string | null;
}

interface ConfigResponse {
  conectado: boolean;
  diasAntesVencimento: number;
  enviarAtrasados: boolean;
}

const ESTADO_CONFIG = {
  open:          { label: "Conectado",      cor: "text-green-600",  bg: "bg-green-50",  borda: "border-green-200" },
  connecting:    { label: "Aguardando scan",cor: "text-yellow-600", bg: "bg-yellow-50", borda: "border-yellow-200"},
  close:         { label: "Desconectado",   cor: "text-red-600",    bg: "bg-red-50",    borda: "border-red-200"   },
  desconectado:  { label: "Não configurado",cor: "text-gray-500",   bg: "bg-gray-50",   borda: "border-gray-200"  },
};

export default function WhatsAppPage() {
  const [conexao, setConexao] = useState<ConexaoResponse | null>(null);
  const [config, setConfig] = useState<ConfigResponse | null>(null);
  const [dias, setDias] = useState(3);
  const [enviarAtrasados, setEnviarAtrasados] = useState(true);
  const [loading, setLoading] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [disparando, setDisparando] = useState(false);
  const [enviados, setEnviados] = useState<number | null>(null);
  const poolRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    carregar();
    return () => { if (poolRef.current) clearInterval(poolRef.current); };
  }, []);

  async function carregar() {
    setLoading(true);
    const [status, cfg] = await Promise.all([
      api.get<ConexaoResponse>("/professor/whatsapp/status"),
      api.get<ConfigResponse>("/professor/whatsapp/configuracao"),
    ]);
    setConexao(status);
    setConfig(cfg);
    setDias(cfg.diasAntesVencimento);
    setEnviarAtrasados(cfg.enviarAtrasados);
    setLoading(false);

    if (status.estado === "connecting") iniciarPolling();
  }

  function iniciarPolling() {
    if (poolRef.current) return;
    poolRef.current = setInterval(async () => {
      const status = await api.get<ConexaoResponse>("/professor/whatsapp/status");
      setConexao(status);
      if (status.estado === "open" || status.estado === "close") {
        clearInterval(poolRef.current!);
        poolRef.current = null;
      }
    }, 4000);
  }

  async function conectar() {
    const res = await api.post<ConexaoResponse>("/professor/whatsapp/conectar", {});
    setConexao(res);
    if (res.estado === "connecting") iniciarPolling();
  }

  async function desconectar() {
    if (!confirm("Desconectar o WhatsApp?")) return;
    await api.delete("/professor/whatsapp/desconectar");
    setConexao({ instanceName: null, estado: "desconectado", qrCodeBase64: null });
  }

  async function salvarConfig() {
    setSalvando(true);
    const res = await api.put<ConfigResponse>("/professor/whatsapp/configuracao", {
      diasAntesVencimento: dias,
      enviarAtrasados,
    });
    setConfig(res);
    setSalvando(false);
  }

  async function dispararManual() {
    setDisparando(true);
    setEnviados(null);
    const res = await api.post<{ enviados: number }>("/professor/whatsapp/lembretes/enviar", {});
    setEnviados(res.enviados);
    setDisparando(false);
  }

  if (loading) return (
    <div className="min-h-screen"><Navbar />
      <div className="text-center py-16 text-gray-400 text-sm">Carregando...</div>
    </div>
  );

  const estadoInfo = ESTADO_CONFIG[conexao?.estado ?? "desconectado"];

  return (
    <div className="min-h-screen">
      <Navbar />

      <main className="max-w-2xl mx-auto px-4 sm:px-6 py-8 space-y-6">
        <h1 className="text-2xl font-bold">WhatsApp</h1>

        {/* Card de status */}
        <div className={`rounded-xl border p-6 ${estadoInfo.bg} ${estadoInfo.borda}`}>
          <div className="flex items-center justify-between mb-4">
            <div>
              <p className="text-xs text-gray-500 mb-0.5">Status da conexão</p>
              <p className={`font-semibold ${estadoInfo.cor}`}>{estadoInfo.label}</p>
            </div>
            {conexao?.estado === "open" ? (
              <button
                onClick={desconectar}
                className="text-sm text-red-500 hover:text-red-700 border border-red-200 rounded-lg px-3 py-1.5 transition-colors"
              >
                Desconectar
              </button>
            ) : conexao?.estado !== "connecting" && (
              <button
                onClick={conectar}
                className="bg-green-600 hover:bg-green-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
              >
                Conectar WhatsApp
              </button>
            )}
          </div>

          {/* QR Code */}
          {conexao?.estado === "connecting" && (
            <div className="flex flex-col items-center gap-3 mt-2">
              {conexao.qrCodeBase64 ? (
                <>
                  <img
                    src={`data:image/png;base64,${conexao.qrCodeBase64}`}
                    alt="QR Code WhatsApp"
                    className="w-48 h-48 rounded-lg border border-white shadow"
                  />
                  <p className="text-sm text-yellow-700 text-center">
                    Abra o WhatsApp no seu celular → Dispositivos Conectados → Conectar dispositivo → Escaneie o QR code
                  </p>
                  <p className="text-xs text-yellow-600 animate-pulse">Aguardando conexão...</p>
                </>
              ) : (
                <p className="text-sm text-yellow-700 animate-pulse">Gerando QR code...</p>
              )}
            </div>
          )}

          {conexao?.estado === "open" && (
            <p className="text-sm text-green-700 mt-2">
              Seu WhatsApp está conectado. Os lembretes automáticos serão enviados diariamente às 09h.
            </p>
          )}
        </div>

        {/* Configurações de envio */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h2 className="font-semibold text-gray-800 mb-4">Configurações de envio automático</h2>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Enviar lembrete quantos dias antes do vencimento?
              </label>
              <div className="flex items-center gap-3">
                <input
                  type="range" min={1} max={15} value={dias}
                  onChange={(e) => setDias(Number(e.target.value))}
                  className="flex-1 accent-brand-600"
                />
                <span className="text-sm font-medium text-brand-700 w-16 text-center">
                  {dias} {dias === 1 ? "dia" : "dias"}
                </span>
              </div>
              <p className="text-xs text-gray-400 mt-1">
                Ex: {dias} → mensalidade que vence dia 10, lembrete enviado no dia {10 - dias > 0 ? 10 - dias : "último mês"}
              </p>
            </div>

            <label className="flex items-center gap-3 cursor-pointer">
              <div
                onClick={() => setEnviarAtrasados(!enviarAtrasados)}
                className={`w-10 h-6 rounded-full transition-colors ${enviarAtrasados ? "bg-brand-600" : "bg-gray-300"}`}
              >
                <div className={`w-4 h-4 bg-white rounded-full shadow mt-1 transition-transform ${enviarAtrasados ? "translate-x-5" : "translate-x-1"}`} />
              </div>
              <span className="text-sm text-gray-700">Enviar lembrete para alunos com mensalidade atrasada</span>
            </label>
          </div>

          <button
            onClick={salvarConfig}
            disabled={salvando}
            className="mt-5 bg-brand-600 hover:bg-brand-700 disabled:opacity-50 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
          >
            {salvando ? "Salvando..." : "Salvar configurações"}
          </button>
        </div>

        {/* Disparo manual */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h2 className="font-semibold text-gray-800 mb-1">Envio manual</h2>
          <p className="text-sm text-gray-500 mb-4">
            Envia lembretes agora para todos os alunos elegíveis (sem aguardar o agendamento diário).
          </p>

          <button
            onClick={dispararManual}
            disabled={disparando || conexao?.estado !== "open"}
            className="bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
          >
            {disparando ? "Enviando..." : "Enviar lembretes agora"}
          </button>

          {enviados !== null && (
            <p className="text-sm text-green-600 mt-3">
              {enviados === 0 ? "Nenhum lembrete a enviar no momento." : `${enviados} lembrete${enviados !== 1 ? "s" : ""} enviado${enviados !== 1 ? "s" : ""} com sucesso.`}
            </p>
          )}

          {conexao?.estado !== "open" && (
            <p className="text-xs text-gray-400 mt-2">Conecte o WhatsApp para habilitar o envio.</p>
          )}
        </div>

        {/* Info sobre Evolution API */}
        <div className="bg-blue-50 border border-blue-100 rounded-xl p-4 text-sm text-blue-700">
          <p className="font-medium mb-1">Como funciona?</p>
          <p>
            O Wellpag usa a{" "}
            <strong>Evolution API</strong> (open source) para enviar mensagens pelo seu próprio número de WhatsApp.
            Configure a URL da Evolution API nas variáveis de ambiente do servidor.
            Nenhum custo por mensagem — você usa sua própria conta WhatsApp.
          </p>
        </div>
      </main>
    </div>
  );
}
