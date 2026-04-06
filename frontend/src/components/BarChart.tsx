import { EvolucaoMensalItem } from "@/lib/relatorio-types";

interface Props {
  dados: EvolucaoMensalItem[];
}

export function BarChart({ dados }: Props) {
  const maxValor = Math.max(...dados.flatMap((d) => [d.esperado, d.recebido]), 1);

  function pct(valor: number) {
    return Math.round((valor / maxValor) * 100);
  }

  function formatMes(mes: string) {
    const [ano, m] = mes.split("-");
    const meses = ["Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez"];
    return meses[parseInt(m) - 1];
  }

  function formatBRL(valor: number) {
    return valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
  }

  return (
    <div>
      {/* Legenda */}
      <div className="flex gap-4 mb-4 text-xs text-gray-500">
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-sm bg-gray-200 inline-block" />
          Esperado
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-sm bg-brand-500 inline-block" />
          Recebido
        </span>
      </div>

      {/* Barras */}
      <div className="flex items-end gap-3 h-40">
        {dados.map((d) => (
          <div key={d.mes} className="flex-1 flex flex-col items-center gap-1 group relative">
            {/* Tooltip */}
            <div className="absolute bottom-full mb-2 hidden group-hover:flex flex-col items-center z-10 pointer-events-none">
              <div className="bg-gray-800 text-white text-xs rounded-lg px-2 py-1.5 whitespace-nowrap shadow-lg">
                <p className="font-medium">{formatMes(d.mes)}</p>
                <p className="text-gray-300">Esperado: {formatBRL(d.esperado)}</p>
                <p className="text-green-300">Recebido: {formatBRL(d.recebido)}</p>
              </div>
              <div className="w-2 h-2 bg-gray-800 rotate-45 -mt-1" />
            </div>

            {/* Par de barras */}
            <div className="w-full flex items-end gap-0.5 h-36">
              <div
                className="flex-1 bg-gray-200 rounded-t transition-all"
                style={{ height: `${pct(d.esperado)}%` }}
              />
              <div
                className="flex-1 bg-brand-500 rounded-t transition-all"
                style={{ height: `${pct(d.recebido)}%` }}
              />
            </div>

            <span className="text-xs text-gray-400">{formatMes(d.mes)}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
