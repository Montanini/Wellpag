"use client";

import { useEffect, useState } from "react";
import { PortalNavbar } from "@/components/PortalNavbar";
import { StatusBadge } from "@/components/StatusBadge";
import { api } from "@/lib/api";
import { MensalidadeResponse, PortalPerfilResponse } from "@/lib/portal-types";

export default function PortalHomePage() {
  const [perfis, setPerfis] = useState<PortalPerfilResponse[]>([]);
  const [mensalidades, setMensalidades] = useState<MensalidadeResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const mesAtual = new Date().toISOString().slice(0, 7); // yyyy-MM

  useEffect(() => {
    Promise.all([
      api.get<PortalPerfilResponse[]>("/aluno/portal/perfil"),
      api.get<MensalidadeResponse[]>(`/aluno/portal/mensalidades/${mesAtual}`),
    ])
      .then(([p, m]) => { setPerfis(p); setMensalidades(m); })
      .finally(() => setLoading(false));
  }, [mesAtual]);

  return (
    <div className="min-h-screen">
      <PortalNavbar />

      <main className="max-w-2xl mx-auto px-4 sm:px-6 py-8 space-y-6">
        {loading ? (
          <div className="text-center py-16 text-gray-400 text-sm">Carregando...</div>
        ) : (
          <>
            {/* Boas-vindas */}
            {perfis.map((perfil) => (
              <div key={perfil.id} className="bg-white rounded-xl border border-gray-200 p-6">
                <h2 className="font-semibold text-gray-800 mb-3">{perfil.nome}</h2>
                <dl className="grid grid-cols-2 gap-3 text-sm">
                  <div><dt className="text-gray-400">E-mail</dt><dd>{perfil.email}</dd></div>
                  <div><dt className="text-gray-400">Telefone</dt><dd>{perfil.telefone ?? "—"}</dd></div>
                  <div>
                    <dt className="text-gray-400">Mensalidade</dt>
                    <dd className="font-medium">
                      {perfil.valorMensalidade != null
                        ? `R$ ${perfil.valorMensalidade.toFixed(2)}`
                        : <span className="text-yellow-600 text-xs">Aguardando definição</span>
                      }
                    </dd>
                  </div>
                  <div>
                    <dt className="text-gray-400">Vencimento</dt>
                    <dd>{perfil.diaVencimento != null ? `Todo dia ${perfil.diaVencimento}` : "—"}</dd>
                  </div>
                </dl>
              </div>
            ))}

            {/* Mensalidade do mês atual */}
            <div>
              <h3 className="text-lg font-semibold mb-3">
                Mensalidade de {mesAtual.replace("-", "/")}
              </h3>
              {mensalidades.length === 0 ? (
                <p className="text-gray-400 text-sm">Nenhuma mensalidade gerada ainda.</p>
              ) : (
                <div className="space-y-3">
                  {mensalidades.map((m) => (
                    <MensalidadeCard key={m.id} m={m} />
                  ))}
                </div>
              )}
            </div>
          </>
        )}
      </main>
    </div>
  );
}

function MensalidadeCard({ m }: { m: MensalidadeResponse }) {
  const bgStatus = {
    PAGO:     "border-green-200 bg-green-50",
    A_PAGAR:  "border-yellow-200 bg-yellow-50",
    ATRASADO: "border-red-200 bg-red-50",
  }[m.status];

  return (
    <div className={`rounded-xl border p-5 ${bgStatus}`}>
      <div className="flex items-center justify-between mb-3">
        <span className="font-medium text-gray-800">{m.mesReferencia.replace("-", "/")}</span>
        <StatusBadge status={m.status} />
      </div>
      <div className="grid grid-cols-2 gap-2 text-sm text-gray-600">
        <div>
          <span className="text-gray-400">Valor</span>
          <p className="font-medium">R$ {m.valor?.toFixed(2) ?? "—"}</p>
        </div>
        <div>
          <span className="text-gray-400">Vencimento</span>
          <p className="font-medium">Dia {m.diaVencimento}</p>
        </div>
        {m.dataPagamento && (
          <div className="col-span-2">
            <span className="text-gray-400">Pago em</span>
            <p className="font-medium">{m.dataPagamento}</p>
          </div>
        )}
        {m.observacao && (
          <div className="col-span-2">
            <span className="text-gray-400">Obs.</span>
            <p>{m.observacao}</p>
          </div>
        )}
      </div>
    </div>
  );
}
