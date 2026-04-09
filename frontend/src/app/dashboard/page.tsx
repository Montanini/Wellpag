"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Navbar } from "@/components/Navbar";
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

const COR_STATUS: Record<string, string> = {
  PAGO:     "bg-[#7ec920] hover:bg-[#6db81a]",
  A_PAGAR:  "bg-yellow-400 hover:bg-yellow-500",
  ATRASADO: "bg-red-500 hover:bg-red-600",
};

function agruparPorHorario(alunos: DashboardAlunoItem[]) {
  const mapa = new Map<string, DashboardAlunoItem[]>();
  for (const aluno of alunos) {
    const chave = aluno.horaInicio.slice(0, 5);
    if (!mapa.has(chave)) mapa.set(chave, []);
    mapa.get(chave)!.push(aluno);
  }
  return Array.from(mapa.entries()).sort(([a], [b]) => a.localeCompare(b));
}

export default function DashboardPage() {
  const router = useRouter();
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

  const grupos = agruparPorHorario(alunos);

  return (
    <div className="min-h-screen bg-gray-50">
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
        <div className="grid grid-cols-3 gap-4 mb-8">
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

        {/* Cards de alunos agrupados por horário */}
        {loading ? (
          <div className="text-center py-16 text-gray-400 text-sm">Carregando...</div>
        ) : erro ? (
          <div className="text-center py-16 text-red-500 text-sm">{erro}</div>
        ) : alunos.length === 0 ? (
          <div className="text-center py-16 text-gray-400 text-sm">
            Nenhum aluno neste horário.
          </div>
        ) : (
          <div className="space-y-8">
            {grupos.map(([horaInicio, grupoAlunos]) => (
              <div key={horaInicio}>
                {/* Separador com horário */}
                <div className="flex items-center gap-4 mb-4">
                  <div className="flex-1 h-px bg-gray-300" />
                  <span className="text-sm font-semibold text-gray-600 whitespace-nowrap">
                    Horário ({horaInicio})
                  </span>
                  <div className="flex-1 h-px bg-gray-300" />
                </div>

                {/* Grid de cards */}
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
                  {grupoAlunos.map((aluno) => (
                    <button
                      key={aluno.alunoId}
                      onClick={() => router.push(`/alunos/${aluno.alunoId}`)}
                      className={`
                        ${COR_STATUS[aluno.statusMensalidade] ?? "bg-gray-400 hover:bg-gray-500"}
                        rounded-2xl px-4 py-6
                        text-white font-semibold text-sm text-center
                        shadow-sm transition-colors duration-150 cursor-pointer
                        min-h-[90px] flex items-center justify-center
                      `}
                    >
                      {aluno.nome}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
