"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { api } from "@/lib/api";

export default function AlunoAutoCadastroPage() {
  const params = useSearchParams();
  const professorId = params.get("professor") ?? "";

  const [form, setForm] = useState({
    nome: "", email: "", telefone: "",
    nomeResponsavel: "", telefoneResponsavel: "",
  });
  const [sucesso, setSucesso] = useState(false);
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);

  function set(field: string, value: string) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!professorId) { setErro("Link inválido — código do professor não encontrado."); return; }

    setErro("");
    setLoading(true);
    try {
      await api.post("/alunos/cadastro", { ...form, professorId });
      setSucesso(true);
    } catch (err: any) {
      setErro(err.message ?? "Erro ao realizar cadastro");
    } finally {
      setLoading(false);
    }
  }

  if (sucesso) {
    return (
      <div className="min-h-screen flex items-center justify-center px-4">
        <div className="text-center max-w-sm">
          <div className="text-5xl mb-4">✓</div>
          <h2 className="text-xl font-semibold text-gray-800">Cadastro realizado!</h2>
          <p className="text-gray-500 text-sm mt-2">
            Seu professor irá confirmar seus horários e mensalidade em breve.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-8">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-brand-600">Wellpag</h1>
          <p className="text-gray-500 mt-1 text-sm">Preencha seus dados para se cadastrar</p>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
          <form onSubmit={handleSubmit} className="space-y-4">
            {[
              { field: "nome",               label: "Nome completo",        type: "text",  required: true,  placeholder: "João Silva" },
              { field: "email",              label: "E-mail",               type: "email", required: true,  placeholder: "seu@email.com" },
              { field: "telefone",           label: "Telefone / WhatsApp",  type: "tel",   required: false, placeholder: "(11) 99999-9999" },
              { field: "nomeResponsavel",    label: "Nome do responsável",  type: "text",  required: false, placeholder: "Opcional (menores)" },
              { field: "telefoneResponsavel",label: "Telefone do responsável", type: "tel", required: false, placeholder: "(11) 99999-9999" },
            ].map(({ field, label, type, required, placeholder }) => (
              <div key={field}>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  {label} {required && <span className="text-red-500">*</span>}
                </label>
                <input
                  type={type}
                  required={required}
                  value={form[field as keyof typeof form]}
                  onChange={(e) => set(field, e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
                  placeholder={placeholder}
                />
              </div>
            ))}

            {erro && <p className="text-red-600 text-sm">{erro}</p>}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-brand-600 hover:bg-brand-700 disabled:opacity-50 text-white font-medium py-2 rounded-lg text-sm transition-colors"
            >
              {loading ? "Enviando..." : "Cadastrar"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
