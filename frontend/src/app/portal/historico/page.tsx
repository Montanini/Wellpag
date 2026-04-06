"use client";

import { useEffect, useState } from "react";
import { PortalNavbar } from "@/components/PortalNavbar";
import { StatusBadge } from "@/components/StatusBadge";
import { api } from "@/lib/api";
import { MensalidadeResponse } from "@/lib/portal-types";

export default function PortalHistoricoPage() {
  const [mensalidades, setMensalidades] = useState<MensalidadeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");

  useEffect(() => {
    api.get<MensalidadeResponse[]>("/aluno/portal/mensalidades")
      .then(setMensalidades)
      .catch((err) => setErro(err.message))
      .finally(() => setLoading(false));
  }, []);

  const totais = {
    pago:     mensalidades.filter((m) => m.status === "PAGO").length,
    aPagar:   mensalidades.filter((m) => m.status === "A_PAGAR").length,
    atrasado: mensalidades.filter((m) => m.status === "ATRASADO").length,
  };

  return (
    <div className="min-h-screen">
      <PortalNavbar />

      <main className="max-w-2xl mx-auto px-4 sm:px-6 py-8 space-y-6">
        <h1 className="text-2xl font-bold">Histórico de Mensalidades</h1>

        {loading ? (
          <div className="text-center py-16 text-gray-400 text-sm">Carregando...</div>
        ) : erro ? (
          <div className="text-center py-16 text-red-500 text-sm">{erro}</div>
        ) : (
          <>
            {/* Cards de resumo */}
            <div className="grid grid-cols-3 gap-3">
              {[
                { label: "Pagos",     count: totais.pago,     color: "text-green-600",  bg: "bg-green-50"  },
                { label: "A pagar",   count: totais.aPagar,   color: "text-yellow-600", bg: "bg-yellow-50" },
                { label: "Atrasados", count: totais.atrasado, color: "text-red-600",    bg: "bg-red-50"    },
              ].map(({ label, count, color, bg }) => (
                <div key={label} className={`${bg} rounded-xl p-4 text-center border border-gray-100`}>
                  <p className={`text-2xl font-bold ${color}`}>{count}</p>
                  <p className="text-xs text-gray-500 mt-1">{label}</p>
                </div>
              ))}
            </div>

            {/* Lista */}
            {mensalidades.length === 0 ? (
              <p className="text-center text-gray-400 text-sm py-8">Nenhuma mensalidade registrada.</p>
            ) : (
              <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 border-b border-gray-200">
                    <tr>
                      <th className="text-left px-4 py-3 font-medium text-gray-600">Mês</th>
                      <th className="text-left px-4 py-3 font-medium text-gray-600 hidden sm:table-cell">Valor</th>
                      <th className="text-left px-4 py-3 font-medium text-gray-600 hidden sm:table-cell">Vencimento</th>
                      <th className="text-left px-4 py-3 font-medium text-gray-600">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {mensalidades.map((m) => (
                      <tr key={m.id} className="hover:bg-gray-50 transition-colors">
                        <td className="px-4 py-3 font-medium">{m.mesReferencia.replace("-", "/")}</td>
                        <td className="px-4 py-3 text-gray-500 hidden sm:table-cell">
                          R$ {m.valor?.toFixed(2) ?? "—"}
                        </td>
                        <td className="px-4 py-3 text-gray-500 hidden sm:table-cell">
                          {m.diaVencimento ? `Dia ${m.diaVencimento}` : "—"}
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex flex-col gap-1">
                            <StatusBadge status={m.status} />
                            {m.dataPagamento && (
                              <span className="text-xs text-gray-400">Pago em {m.dataPagamento}</span>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
