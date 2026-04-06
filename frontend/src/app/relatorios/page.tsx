"use client";

import { useEffect, useState } from "react";
import { Navbar } from "@/components/Navbar";
import { BarChart } from "@/components/BarChart";
import { api } from "@/lib/api";
import {
  EvolucaoMensalItem,
  InadimplenteItem,
  RelatorioResumoResponse,
} from "@/lib/relatorio-types";

const MESES_DISPONIVEIS = Array.from({ length: 12 }, (_, i) => {
  const d = new Date();
  d.setMonth(d.getMonth() - i);
  return d.toISOString().slice(0, 7);
});

function formatBRL(valor: number) {
  return valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

export default function RelatoriosPage() {
  const [mesSelecionado, setMesSelecionado] = useState(MESES_DISPONIVEIS[0]);
  const [resumo, setResumo] = useState<RelatorioResumoResponse | null>(null);
  const [evolucao, setEvolucao] = useState<EvolucaoMensalItem[]>([]);
  const [inadimplentes, setInadimplentes] = useState<InadimplenteItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      api.get<RelatorioResumoResponse>(`/professor/relatorios/resumo?mes=${mesSelecionado}`),
      api.get<EvolucaoMensalItem[]>("/professor/relatorios/evolucao?meses=6"),
      api.get<InadimplenteItem[]>(`/professor/relatorios/inadimplentes?mes=${mesSelecionado}`),
    ])
      .then(([r, e, i]) => { setResumo(r); setEvolucao(e); setInadimplentes(i); })
      .finally(() => setLoading(false));
  }, [mesSelecionado]);

  return (
    <div className="min-h-screen">
      <Navbar />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <h1 className="text-2xl font-bold">Relatórios Financeiros</h1>
          <select
            value={mesSelecionado}
            onChange={(e) => setMesSelecionado(e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
          >
            {MESES_DISPONIVEIS.map((m) => (
              <option key={m} value={m}>{m.replace("-", "/")}</option>
            ))}
          </select>
        </div>

        {loading ? (
          <div className="text-center py-16 text-gray-400 text-sm">Carregando...</div>
        ) : (
          <>
            {/* Cards de resumo */}
            {resumo && (
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
                {[
                  { label: "Total de alunos",  valor: resumo.totalAlunos,           tipo: "numero",  cor: "text-gray-800"   },
                  { label: "Receita esperada", valor: resumo.totalEsperado,         tipo: "moeda",   cor: "text-gray-800"   },
                  { label: "Recebido",         valor: resumo.totalRecebido,         tipo: "moeda",   cor: "text-green-600"  },
                  { label: "A receber",        valor: resumo.totalAPagar,           tipo: "moeda",   cor: "text-yellow-600" },
                  { label: "Atrasado",         valor: resumo.totalAtrasado,         tipo: "moeda",   cor: "text-red-600"    },
                ].map(({ label, valor, tipo, cor }) => (
                  <div key={label} className="bg-white rounded-xl border border-gray-200 p-4">
                    <p className="text-xs text-gray-400 mb-1">{label}</p>
                    <p className={`text-xl font-bold ${cor}`}>
                      {tipo === "moeda" ? formatBRL(valor as number) : valor}
                    </p>
                  </div>
                ))}
              </div>
            )}

            {/* Barra de progresso de recebimento */}
            {resumo && (
              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-gray-700">Taxa de recebimento</span>
                  <span className={`text-sm font-bold ${
                    resumo.percentualRecebido >= 80 ? "text-green-600" :
                    resumo.percentualRecebido >= 50 ? "text-yellow-600" : "text-red-600"
                  }`}>
                    {resumo.percentualRecebido.toFixed(1)}%
                  </span>
                </div>
                <div className="w-full bg-gray-100 rounded-full h-3">
                  <div
                    className={`h-3 rounded-full transition-all ${
                      resumo.percentualRecebido >= 80 ? "bg-green-500" :
                      resumo.percentualRecebido >= 50 ? "bg-yellow-500" : "bg-red-500"
                    }`}
                    style={{ width: `${Math.min(resumo.percentualRecebido, 100)}%` }}
                  />
                </div>
                <div className="flex justify-between mt-1 text-xs text-gray-400">
                  <span>{formatBRL(resumo.totalRecebido)} recebido</span>
                  <span>meta: {formatBRL(resumo.totalEsperado)}</span>
                </div>
              </div>
            )}

            {/* Gráfico de evolução */}
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h2 className="font-semibold text-gray-800 mb-4">Evolução dos últimos 6 meses</h2>
              {evolucao.length > 0 ? (
                <BarChart dados={evolucao} />
              ) : (
                <p className="text-gray-400 text-sm">Sem dados suficientes.</p>
              )}
            </div>

            {/* Inadimplentes */}
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h2 className="font-semibold text-gray-800 mb-4">
                Inadimplentes em {mesSelecionado.replace("-", "/")}
                {inadimplentes.length > 0 && (
                  <span className="ml-2 text-sm font-normal text-red-500">
                    ({inadimplentes.length} aluno{inadimplentes.length !== 1 ? "s" : ""})
                  </span>
                )}
              </h2>

              {inadimplentes.length === 0 ? (
                <div className="flex items-center gap-2 text-green-600 text-sm">
                  <span>✓</span>
                  <span>Nenhum inadimplente neste mês.</span>
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="border-b border-gray-100">
                      <tr>
                        <th className="text-left py-2 font-medium text-gray-600">Aluno</th>
                        <th className="text-left py-2 font-medium text-gray-600 hidden sm:table-cell">Telefone</th>
                        <th className="text-left py-2 font-medium text-gray-600 hidden sm:table-cell">Meses atrasados</th>
                        <th className="text-right py-2 font-medium text-gray-600">Total em aberto</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-50">
                      {inadimplentes.map((item) => (
                        <tr key={item.alunoId} className="hover:bg-gray-50 transition-colors">
                          <td className="py-3 font-medium">{item.nome}</td>
                          <td className="py-3 text-gray-500 hidden sm:table-cell">
                            {item.telefone ?? "—"}
                          </td>
                          <td className="py-3 text-gray-500 hidden sm:table-cell">
                            {item.mesesAtrasados} {item.mesesAtrasados === 1 ? "mês" : "meses"}
                          </td>
                          <td className="py-3 text-right font-medium text-red-600">
                            {formatBRL(item.totalAtrasado)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot className="border-t border-gray-200">
                      <tr>
                        <td colSpan={3} className="pt-3 text-sm text-gray-500 hidden sm:table-cell">
                          Total em atraso
                        </td>
                        <td colSpan={1} className="pt-3 text-sm text-gray-500 table-cell sm:hidden">
                          Total em atraso
                        </td>
                        <td className="pt-3 text-right font-bold text-red-600">
                          {formatBRL(inadimplentes.reduce((s, i) => s + i.totalAtrasado, 0))}
                        </td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              )}
            </div>
          </>
        )}
      </main>
    </div>
  );
}
