"use client";

import { useEffect, useState } from "react";
import { Navbar } from "@/components/Navbar";
import { api } from "@/lib/api";
import { Aluno, DiaSemana, Horario, TipoHorario } from "@/lib/types";

const DIAS: { value: DiaSemana; label: string }[] = [
  { value: "SEGUNDA", label: "Segunda" },
  { value: "TERCA",   label: "Terça"   },
  { value: "QUARTA",  label: "Quarta"  },
  { value: "QUINTA",  label: "Quinta"  },
  { value: "SEXTA",   label: "Sexta"   },
  { value: "SABADO",  label: "Sábado"  },
  { value: "DOMINGO", label: "Domingo" },
];

const DIA_LABEL: Record<DiaSemana, string> = {
  SEGUNDA: "Segunda", TERCA: "Terça", QUARTA: "Quarta",
  QUINTA: "Quinta", SEXTA: "Sexta", SABADO: "Sábado", DOMINGO: "Domingo",
};

const FORM_VAZIO = { alunoId: "", diaSemana: "" as DiaSemana | "", horaInicio: "", horaFim: "", tipo: "FIXO" as TipoHorario };

export default function HorariosPage() {
  const [horarios, setHorarios] = useState<Horario[]>([]);
  const [alunos, setAlunos] = useState<Aluno[]>([]);
  const [form, setForm] = useState(FORM_VAZIO);
  const [adicionando, setAdicionando] = useState(false);
  const [loading, setLoading] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");

  useEffect(() => {
    Promise.all([
      api.get<Horario[]>("/professor/horarios"),
      api.get<Aluno[]>("/professor/alunos"),
    ])
      .then(([h, a]) => { setHorarios(h); setAlunos(a); })
      .catch((err) => setErro(err.message))
      .finally(() => setLoading(false));
  }, []);

  function set(field: string, value: string) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function salvar(e: React.FormEvent) {
    e.preventDefault();
    setSalvando(true);
    try {
      const novo = await api.post<Horario>("/professor/horarios", form);
      setHorarios((prev) => [...prev, novo]);
      setForm(FORM_VAZIO);
      setAdicionando(false);
    } catch (err: any) {
      setErro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  async function remover(horarioId: string) {
    if (!confirm("Remover este horário?")) return;
    try {
      await api.delete(`/professor/horarios/${horarioId}`);
      setHorarios((prev) => prev.filter((h) => h.id !== horarioId));
    } catch (err: any) {
      alert(err.message);
    }
  }

  function nomeAluno(alunoId: string) {
    return alunos.find((a) => a.id === alunoId)?.nome ?? alunoId;
  }

  // Agrupa por dia da semana
  const porDia = DIAS.map(({ value, label }) => ({
    dia: value,
    label,
    horarios: horarios
      .filter((h) => h.diaSemana === value)
      .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio)),
  })).filter((d) => d.horarios.length > 0);

  return (
    <div className="min-h-screen">
      <Navbar />

      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold">Horários</h1>
          <button
            onClick={() => setAdicionando(!adicionando)}
            className="bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
          >
            {adicionando ? "Cancelar" : "+ Novo horário"}
          </button>
        </div>

        {/* Formulário de novo horário */}
        {adicionando && (
          <form onSubmit={salvar} className="bg-white rounded-xl border border-gray-200 p-6 mb-6">
            <h2 className="font-semibold mb-4">Novo horário</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs text-gray-500 mb-1">Aluno</label>
                <select required value={form.alunoId} onChange={(e) => set("alunoId", e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white">
                  <option value="">Selecione...</option>
                  {alunos.map((a) => <option key={a.id} value={a.id}>{a.nome}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Dia da semana</label>
                <select required value={form.diaSemana} onChange={(e) => set("diaSemana", e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white">
                  <option value="">Selecione...</option>
                  {DIAS.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Hora início</label>
                <input type="time" required value={form.horaInicio} onChange={(e) => set("horaInicio", e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500" />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Hora fim</label>
                <input type="time" required value={form.horaFim} onChange={(e) => set("horaFim", e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500" />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Tipo</label>
                <select value={form.tipo} onChange={(e) => set("tipo", e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white">
                  <option value="FIXO">Fixo (semanal)</option>
                  <option value="AVULSO">Avulso</option>
                </select>
              </div>
            </div>
            {erro && <p className="text-red-600 text-sm mt-3">{erro}</p>}
            <button type="submit" disabled={salvando}
              className="mt-4 bg-brand-600 hover:bg-brand-700 disabled:opacity-50 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors">
              {salvando ? "Salvando..." : "Salvar horário"}
            </button>
          </form>
        )}

        {/* Lista agrupada por dia */}
        {loading ? (
          <div className="text-center py-16 text-gray-400 text-sm">Carregando...</div>
        ) : porDia.length === 0 ? (
          <div className="text-center py-16 text-gray-400 text-sm">Nenhum horário cadastrado.</div>
        ) : (
          <div className="space-y-4">
            {porDia.map(({ dia, label, horarios: lista }) => (
              <div key={dia} className="bg-white rounded-xl border border-gray-200 overflow-hidden">
                <div className="bg-gray-50 border-b border-gray-200 px-4 py-2">
                  <h3 className="font-medium text-gray-700 text-sm">{label}</h3>
                </div>
                <table className="w-full text-sm">
                  <tbody className="divide-y divide-gray-50">
                    {lista.map((h) => (
                      <tr key={h.id} className="hover:bg-gray-50 transition-colors">
                        <td className="px-4 py-3 font-medium">{nomeAluno(h.alunoId)}</td>
                        <td className="px-4 py-3 text-gray-500">
                          {h.horaInicio.slice(0, 5)} – {h.horaFim.slice(0, 5)}
                        </td>
                        <td className="px-4 py-3 text-gray-400 text-xs hidden sm:table-cell">
                          {h.tipo === "FIXO" ? "Fixo" : "Avulso"}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button onClick={() => remover(h.id)}
                            className="text-red-400 hover:text-red-600 text-xs transition-colors">
                            Remover
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
