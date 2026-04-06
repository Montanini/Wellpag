"use client";

import { useEffect, useState } from "react";
import { PortalNavbar } from "@/components/PortalNavbar";
import { StatusBadge } from "@/components/StatusBadge";
import { api } from "@/lib/api";
import { PortalRelatorioResponse } from "@/lib/relatorio-types";
import { MensalidadeResponse } from "@/lib/portal-types";

function formatBRL(valor: number) {
  return valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

export default function PortalRelatorioPage() {
  const [relatorio, setRelatorio] = useState<PortalRelatorioResponse | null>(null);
  const [mensalidades, setMensalidades] = useState<MensalidadeResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.get<PortalRelatorioResponse>("/aluno/portal/relatorio"),
      api.get<MensalidadeResponse[]>("/aluno/portal/mensalidades"),
    ])
      .then(([r, m]) => { setRelatorio(r); setMensalidades(m); })
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="min-h-screen">
      <PortalNavbar />

      <main className="max-w-2xl mx-auto px-4 sm:px-6 py-8 space-y-6">
        <h1 className="text-2xl font-bold">Meu Relatório Financeiro</h1>

        {loading ? (
          <div className="text-center py-16 text-gray-400 text-sm">Carregando...</div>
        ) : (
          <>
            {/* Cards de resumo */}
            {relatorio && (
              <>
                <div className="grid grid-cols-2 gap-3">
                  {[
                    { label: "Total pago",    valor: relatorio.totalPago,     cor: "text-green-600",  bg: "bg-green-50",  borda: "border-green-100"  },
                    { label: "A pagar",       valor: relatorio.totalAPagar,   cor: "text-yellow-600", bg: "bg-yellow-50", borda: "border-yellow-100" },
                    { label: "Atrasado",      valor: relatorio.totalAtrasado, cor: "text-red-600",    bg: "bg-red-50",    borda: "border-red-100"    },
                    { label: "Total geral",   valor: relatorio.totalGeral,    cor: "text-gray-800",   bg: "bg-gray-50",   borda: "border-gray-100"   },
                  ].map(({ label, valor, cor, bg, borda }) => (
                    <div key={label} className={`${bg} border ${borda} rounded-xl p-4`}>
                      <p className="text-xs text-gray-400 mb-1">{label}</p>
                      <p className={`text-xl font-bold ${cor}`}>{formatBRL(valor)}</p>
                    </div>
                  ))}
                </div>

                {/* Barra de progresso */}
                {relatorio.totalGeral > 0 && (
                  <div className="bg-white rounded-xl border border-gray-200 p-5">
                    <p className="text-sm font-medium text-gray-700 mb-3">Situação dos pagamentos</p>
                    <div className="w-full h-4 rounded-full bg-gray-100 overflow-hidden flex">
                      {relatorio.totalPago > 0 && (
                        <div
                          className="bg-green-500 h-full transition-all"
                          style={{ width: `${(relatorio.totalPago / relatorio.totalGeral) * 100}%` }}
                          title="Pago"
                        />
                      )}
                      {relatorio.totalAPagar > 0 && (
                        <div
                          className="bg-yellow-400 h-full transition-all"
                          style={{ width: `${(relatorio.totalAPagar / relatorio.totalGeral) * 100}%` }}
                          title="A pagar"
                        />
                      )}
                      {relatorio.totalAtrasado > 0 && (
                        <div
                          className="bg-red-500 h-full transition-all"
                          style={{ width: `${(relatorio.totalAtrasado / relatorio.totalGeral) * 100}%` }}
                          title="Atrasado"
                        />
                      )}
                    </div>
                    <div className="flex gap-4 mt-2 text-xs text-gray-400">
                      <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-green-500 inline-block"/>Pago</span>
                      <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-yellow-400 inline-block"/>A pagar</span>
                      <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-red-500 inline-block"/>Atrasado</span>
                    </div>
                  </div>
                )}

                <p className="text-xs text-gray-400">
                  Total de {relatorio.totalMeses} mensalidade{relatorio.totalMeses !== 1 ? "s" : ""} registrada{relatorio.totalMeses !== 1 ? "s" : ""}.
                </p>
              </>
            )}

            {/* Histórico detalhado */}
            <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
              <div className="px-4 py-3 border-b border-gray-100 bg-gray-50">
                <h2 className="font-semibold text-gray-800 text-sm">Histórico detalhado</h2>
              </div>

              {mensalidades.length === 0 ? (
                <p className="text-gray-400 text-sm p-4">Nenhuma mensalidade registrada.</p>
              ) : (
                <table className="w-full text-sm">
                  <thead className="border-b border-gray-100">
                    <tr>
                      <th className="text-left px-4 py-2 font-medium text-gray-500">Mês</th>
                      <th className="text-left px-4 py-2 font-medium text-gray-500 hidden sm:table-cell">Valor</th>
                      <th className="text-left px-4 py-2 font-medium text-gray-500">Status</th>
                      <th className="text-left px-4 py-2 font-medium text-gray-500 hidden sm:table-cell">Pago em</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50">
                    {mensalidades.map((m) => (
                      <tr key={m.id} className="hover:bg-gray-50 transition-colors">
                        <td className="px-4 py-3 font-medium">{m.mesReferencia.replace("-", "/")}</td>
                        <td className="px-4 py-3 text-gray-500 hidden sm:table-cell">
                          {m.valor != null ? formatBRL(m.valor) : "—"}
                        </td>
                        <td className="px-4 py-3">
                          <StatusBadge status={m.status} />
                        </td>
                        <td className="px-4 py-3 text-gray-400 text-xs hidden sm:table-cell">
                          {m.dataPagamento ?? "—"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        )}
      </main>
    </div>
  );
}
