"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Navbar } from "@/components/Navbar";
import { StatusBadge } from "@/components/StatusBadge";
import { api } from "@/lib/api";
import { Aluno, Mensalidade } from "@/lib/types";

export default function AlunoDetalhePage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();

  const [aluno, setAluno] = useState<Aluno | null>(null);
  const [mensalidades, setMensalidades] = useState<Mensalidade[]>([]);
  const [enviandoLembrete, setEnviandoLembrete] = useState(false);
  const [editando, setEditando] = useState(false);
  const [form, setForm] = useState({ valorMensalidade: "", diaVencimento: "", cpfPagador: "" });
  const [loading, setLoading] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const [alterandoStatusId, setAlterandoStatusId] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      api.get<Aluno>(`/professor/alunos/${id}`),
      api.get<Mensalidade[]>(`/professor/mensalidades/aluno/${id}`),
    ])
      .then(([a, m]) => {
        setAluno(a);
        setMensalidades(m);
        setForm({
          valorMensalidade: a.valorMensalidade?.toString() ?? "",
          diaVencimento:    a.diaVencimento?.toString() ?? "",
          cpfPagador:       a.cpfPagador ?? "",
        });
      })
      .catch((err) => setErro(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  async function salvarComplemento(e: React.FormEvent) {
    e.preventDefault();
    setSalvando(true);
    try {
      const atualizado = await api.patch<Aluno>(`/professor/alunos/${id}/complemento`, {
        valorMensalidade: parseFloat(form.valorMensalidade),
        diaVencimento:    parseInt(form.diaVencimento),
        cpfPagador:       form.cpfPagador || null,
      });
      setAluno(atualizado);
      setEditando(false);
    } catch (err: any) {
      setErro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  async function enviarLembrete() {
    setEnviandoLembrete(true);
    try {
      await api.post(`/professor/whatsapp/lembretes/aluno/${id}`, {});
      alert("Lembrete enviado via WhatsApp!");
    } catch (err: any) {
      alert(err.message ?? "Erro ao enviar lembrete");
    } finally {
      setEnviandoLembrete(false);
    }
  }

  async function confirmarPagamento(mensalidadeId: string) {
    const dataPagamento = new Date().toISOString().slice(0, 10);
    try {
      const atualizada = await api.patch<Mensalidade>(
        `/professor/mensalidades/${mensalidadeId}/confirmar`,
        { dataPagamento }
      );
      setMensalidades((prev) => prev.map((m) => m.id === mensalidadeId ? atualizada : m));
    } catch (err: any) {
      alert(err.message);
    }
  }

  async function alterarStatus(mensalidadeId: string, status: string) {
    try {
      const atualizada = await api.patch<Mensalidade>(
        `/professor/mensalidades/${mensalidadeId}/status`,
        { status }
      );
      setMensalidades((prev) => prev.map((m) => m.id === mensalidadeId ? atualizada : m));
    } catch (err: any) {
      alert(err.message);
    } finally {
      setAlterandoStatusId(null);
    }
  }

  if (loading) return <div className="min-h-screen"><Navbar /><div className="text-center py-16 text-gray-400 text-sm">Carregando...</div></div>;
  if (!aluno)  return <div className="min-h-screen"><Navbar /><div className="text-center py-16 text-red-500 text-sm">{erro}</div></div>;

  return (
    <div className="min-h-screen">
      <Navbar />

      <main className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        {/* Cabeçalho */}
        <div className="flex items-center gap-4">
          <button onClick={() => router.back()} className="text-gray-400 hover:text-gray-600 text-sm">← Voltar</button>
          <h1 className="text-2xl font-bold flex-1">{aluno.nome}</h1>
        </div>

        {/* Dados do aluno */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-gray-800">Dados</h2>
            <button onClick={() => setEditando(!editando)} className="text-brand-600 text-sm hover:underline">
              {editando ? "Cancelar" : "Editar mensalidade"}
            </button>
          </div>

          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
            <div><dt className="text-gray-500">E-mail</dt><dd className="font-medium break-all">{aluno.email}</dd></div>
            <div><dt className="text-gray-500">Telefone</dt><dd className="font-medium">{aluno.telefone ?? "—"}</dd></div>
            <div>
              <dt className="text-gray-500">CPF do Pagador PIX</dt>
              <dd className="font-medium">{aluno.cpfPagador ?? <span className="text-yellow-600 text-xs">Não informado — PIX não será vinculado automaticamente</span>}</dd>
            </div>
            {aluno.nomeResponsavel && (
              <div className="col-span-2">
                <dt className="text-gray-500">Responsável</dt>
                <dd className="font-medium">{aluno.nomeResponsavel} — {aluno.telefoneResponsavel}</dd>
              </div>
            )}
          </dl>

          {editando ? (
            <form onSubmit={salvarComplemento} className="mt-4 space-y-3">
              <div className="flex flex-col sm:flex-row gap-3">
                <div className="flex-1">
                  <label className="block text-xs text-gray-500 mb-1">Valor mensalidade (R$)</label>
                  <input
                    type="number" step="0.01" min="0" required
                    value={form.valorMensalidade}
                    onChange={(e) => setForm((f) => ({ ...f, valorMensalidade: e.target.value }))}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
                  />
                </div>
                <div className="flex-1">
                  <label className="block text-xs text-gray-500 mb-1">Dia de vencimento</label>
                  <input
                    type="number" min="1" max="28" required
                    value={form.diaVencimento}
                    onChange={(e) => setForm((f) => ({ ...f, diaVencimento: e.target.value }))}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
                  />
                </div>
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">CPF do Pagador PIX</label>
                <input
                  type="text"
                  value={form.cpfPagador}
                  onChange={(e) => setForm((f) => ({ ...f, cpfPagador: e.target.value }))}
                  placeholder="CPF de quem faz o PIX (pode ser do responsável)"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
                />
                <p className="text-xs text-gray-400 mt-1">Se o aluno pagar por outro CPF, dê baixa manualmente nas notificações.</p>
              </div>
              <div>
                <button
                  type="submit" disabled={salvando}
                  className="bg-brand-600 hover:bg-brand-700 disabled:opacity-50 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
                >
                  {salvando ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </form>
          ) : (
            <dl className="grid grid-cols-2 gap-3 text-sm mt-4 pt-4 border-t border-gray-100">
              <div>
                <dt className="text-gray-500">Mensalidade</dt>
                <dd className="font-medium">
                  {aluno.valorMensalidade != null ? `R$ ${aluno.valorMensalidade.toFixed(2)}` : <span className="text-yellow-600">Não definido</span>}
                </dd>
              </div>
              <div>
                <dt className="text-gray-500">Vencimento</dt>
                <dd className="font-medium">{aluno.diaVencimento != null ? `Dia ${aluno.diaVencimento}` : "—"}</dd>
              </div>
            </dl>
          )}
        </div>

        {/* Histórico de mensalidades */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h2 className="font-semibold text-gray-800 mb-4">Mensalidades</h2>

          {mensalidades.length === 0 ? (
            <p className="text-gray-400 text-sm">Nenhuma mensalidade registrada.</p>
          ) : (
            <div className="space-y-2">
              {mensalidades.map((m) => (
                <div key={m.id} className="py-2 border-b border-gray-50 last:border-0">
                  <div className="flex items-center justify-between gap-2">
                    <div>
                      <p className="text-sm font-medium">{m.mesReferencia}</p>
                      <p className="text-xs text-gray-400">
                        Venc. dia {m.diaVencimento} · R$ {m.valor?.toFixed(2)}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 flex-shrink-0">
                      <StatusBadge status={m.status} />
                      <button
                        onClick={() => setAlterandoStatusId(alterandoStatusId === m.id ? null : m.id)}
                        className="text-xs text-gray-500 border border-gray-200 rounded px-2 py-1 hover:bg-gray-50"
                      >
                        Alterar status
                      </button>
                    </div>
                  </div>

                  {/* Seletor inline de status */}
                  {alterandoStatusId === m.id && (
                    <div className="mt-2 flex gap-2 flex-wrap">
                      {(["PAGO", "A_PAGAR", "ATRASADO"] as const).map((s) => (
                        <button
                          key={s}
                          onClick={() => alterarStatus(m.id, s)}
                          disabled={m.status === s}
                          className={`text-xs px-3 py-1.5 rounded-lg border font-medium transition-colors disabled:opacity-40 disabled:cursor-default
                            ${s === "PAGO"     ? "bg-green-50  border-green-200  text-green-700  hover:bg-green-100"  : ""}
                            ${s === "A_PAGAR"  ? "bg-yellow-50 border-yellow-200 text-yellow-700 hover:bg-yellow-100" : ""}
                            ${s === "ATRASADO" ? "bg-red-50    border-red-200    text-red-700    hover:bg-red-100"    : ""}
                          `}
                        >
                          {s === "PAGO" ? "Pago" : s === "A_PAGAR" ? "A pagar" : "Atrasado"}
                        </button>
                      ))}
                      <button
                        onClick={() => setAlterandoStatusId(null)}
                        className="text-xs px-3 py-1.5 rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50"
                      >
                        Cancelar
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          {/* Botão Enviar lembrete abaixo das mensalidades */}
          <div className="mt-4 pt-4 border-t border-gray-100">
            <button
              onClick={enviarLembrete}
              disabled={enviandoLembrete}
              className="text-sm text-green-700 border border-green-200 bg-green-50 hover:bg-green-100 disabled:opacity-50 rounded-lg px-4 py-2 transition-colors"
            >
              {enviandoLembrete ? "Enviando..." : "Enviar lembrete"}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
