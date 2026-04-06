import { StatusMensalidade } from "@/lib/types";

const config: Record<StatusMensalidade, { label: string; className: string }> = {
  PAGO:     { label: "Pago",     className: "bg-green-100 text-green-800 border border-green-200" },
  A_PAGAR:  { label: "A pagar",  className: "bg-yellow-100 text-yellow-800 border border-yellow-200" },
  ATRASADO: { label: "Atrasado", className: "bg-red-100 text-red-800 border border-red-200" },
};

export function StatusBadge({ status }: { status: StatusMensalidade }) {
  const { label, className } = config[status];
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${className}`}>
      {label}
    </span>
  );
}
