"use client";

import { useEffect, useState } from "react";
import { PortalNavbar } from "@/components/PortalNavbar";
import { api } from "@/lib/api";
import { Horario, DiaSemana } from "@/lib/types";

const DIA_LABEL: Record<DiaSemana, string> = {
  SEGUNDA: "Segunda-feira",
  TERCA:   "Terça-feira",
  QUARTA:  "Quarta-feira",
  QUINTA:  "Quinta-feira",
  SEXTA:   "Sexta-feira",
  SABADO:  "Sábado",
  DOMINGO: "Domingo",
};

const DIA_ORDER: DiaSemana[] = ["SEGUNDA", "TERCA", "QUARTA", "QUINTA", "SEXTA", "SABADO", "DOMINGO"];

export default function PortalHorariosPage() {
  const [horarios, setHorarios] = useState<Horario[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");

  useEffect(() => {
    api.get<Horario[]>("/aluno/portal/horarios")
      .then(setHorarios)
      .catch((err) => setErro(err.message))
      .finally(() => setLoading(false));
  }, []);

  const porDia = DIA_ORDER.map((dia) => ({
    dia,
    label: DIA_LABEL[dia],
    horarios: horarios
      .filter((h) => h.diaSemana === dia)
      .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio)),
  })).filter((d) => d.horarios.length > 0);

  return (
    <div className="min-h-screen">
      <PortalNavbar />

      <main className="max-w-2xl mx-auto px-4 sm:px-6 py-8 space-y-6">
        <h1 className="text-2xl font-bold">Meus Horários</h1>

        {loading ? (
          <div className="text-center py-16 text-gray-400 text-sm">Carregando...</div>
        ) : erro ? (
          <div className="text-center py-16 text-red-500 text-sm">{erro}</div>
        ) : porDia.length === 0 ? (
          <div className="text-center py-16 text-gray-400 text-sm">
            Nenhum horário cadastrado ainda.
          </div>
        ) : (
          <div className="space-y-4">
            {porDia.map(({ dia, label, horarios: lista }) => (
              <div key={dia} className="bg-white rounded-xl border border-gray-200 overflow-hidden">
                <div className="bg-gray-50 border-b border-gray-200 px-4 py-2">
                  <h3 className="font-medium text-gray-700 text-sm">{label}</h3>
                </div>
                <div className="divide-y divide-gray-50">
                  {lista.map((h) => (
                    <div key={h.id} className="flex items-center justify-between px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="w-2 h-2 rounded-full bg-brand-500" />
                        <span className="text-sm font-medium">
                          {h.horaInicio.slice(0, 5)} – {h.horaFim.slice(0, 5)}
                        </span>
                      </div>
                      <span className="text-xs text-gray-400">
                        {h.tipo === "FIXO" ? "Semanal" : "Avulso"}
                      </span>
                    </div>
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
