"use client";

import { useEffect, useState } from "react";
import { Navbar } from "@/components/Navbar";
import { StatusBadge } from "@/components/StatusBadge";
import { api } from "@/lib/api";
import { DashboardAlunoItem, DiaSemana } from "@/lib/types";

const DIAS: { value: DiaSemana; label: string }[] = [
  { value: "SEGUNDA", label: "Segunda" },
  { value: "TERCA",   label: "Terça"   },
  { value: "QUARTA",  label: "Quarta"  },
  { value: "QUINTA",  label: "Quinta"  },
  { value: "SEXTA",   label: "Sexta"   },
  { value: "SABADO",  label: "Sábado"  },
  { value: "DOMINGO", label: "Domingo" },
];

export default function DashboardPage() {
  const [alunos, setAlunos] = useState<DashboardAlunoItem[]>([]);
  const [diaSemana, setDiaSemana] = useState<DiaSemana | "">("");
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");

  useEffect(() => {
    carregar(diaSemana || undefined);
  }, [diaSemana]);

  async function carregar(dia?: string) {
    setLoading(true);
    setErro("");
    try {
      const query = dia ? `?diaSemana=${dia}` : "";
      const data = await api.get<DashboardAlunoItem[]>(`/professor/dashboard${query}`);
      setAlunos(data);
    } catch (err: any) {
      setErro(err.message);
    } finally {
      setLoading(false);
    }
  }

  const totais = {
    pago:     alunos.filter((a) => a.statusMensalidade === "PAGO").length,
    aPagar:   alunos.filter((a) => a.statusMensalidade === "A_PAGAR").length,
    atrasado: alunos.filter((a) => a.statusMensalidade === "ATRASADO").length,
  };

  return (
    <div className="min-h-screen">
      <Navbar />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
          <h1 className="text-2xl font-bold">Dashboard</h1>

          <select
            value={diaSemana}
            onChange={(e) => setDiaSemana(e.target.value as DiaSemana)}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
          >
            <option value="">Hoje (padrão)</option>
            {DIAS.map((d) => (
              <option key={d.value} value={d.value}>{d.label}</option>
            ))}
          </select>
        </div>

        {/* Cards de resumo */}
        <div className="grid grid-cols-3 gap-4 mb-6">
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

        {/* Tabela */}
        {loading ? (
          <div className="text-center py-16 text-gray-400 text-sm">Carregando...</div>
        ) : erro ? (
          <div className="text-center py-16 text-red-500 text-sm">{erro}</div>
        ) : alunos.length === 0 ? (
          <div className="text-center py-16 text-gray-400 text-sm">
            Nenhum aluno neste horário.
          </div>
        ) : (
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Aluno</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600 hidden sm:table-cell">Horário</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600 hidden sm:table-cell">Telefone</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Mensalidade</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {alunos.map((aluno) => (
                  <tr key={aluno.alunoId} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 font-medium">{aluno.nome}</td>
                    <td className="px-4 py-3 text-gray-500 hidden sm:table-cell">
                      {aluno.horaInicio.slice(0, 5)} – {aluno.horaFim.slice(0, 5)}
                    </td>
                    <td className="px-4 py-3 text-gray-500 hidden sm:table-cell">
                      {aluno.telefone ?? "—"}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={aluno.statusMensalidade} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
