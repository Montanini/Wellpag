"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { saveAuth, AuthUser } from "@/lib/auth";

export default function CadastroPage() {
  const router = useRouter();
  const [form, setForm] = useState({ nome: "", email: "", senha: "", confirmar: "" });
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);

  function set(field: string, value: string) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErro("");

    if (form.senha !== form.confirmar) {
      setErro("As senhas não coincidem");
      return;
    }

    setLoading(true);
    try {
      const data = await api.post<AuthUser>("/auth/registrar", {
        nome: form.nome,
        email: form.email,
        senha: form.senha,
      });
      saveAuth(data);
      router.push(data.role === "ALUNO" ? "/portal" : "/dashboard");
    } catch (err: any) {
      setErro(err.message ?? "Erro ao criar conta");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-brand-600">Wellpag</h1>
          <p className="text-gray-500 mt-1 text-sm">Crie sua conta de professor</p>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
          <h2 className="text-xl font-semibold mb-6">Criar conta</h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            {[
              { field: "nome",      label: "Nome completo", type: "text",     placeholder: "João Silva" },
              { field: "email",     label: "E-mail",        type: "email",    placeholder: "seu@email.com" },
              { field: "senha",     label: "Senha",         type: "password", placeholder: "mín. 8 caracteres" },
              { field: "confirmar", label: "Confirmar senha", type: "password", placeholder: "••••••••" },
            ].map(({ field, label, type, placeholder }) => (
              <div key={field}>
                <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
                <input
                  type={type}
                  required
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
              {loading ? "Criando conta..." : "Criar conta"}
            </button>
          </form>

          <p className="text-center text-sm text-gray-500 mt-6">
            Já tem conta?{" "}
            <Link href="/login" className="text-brand-600 hover:underline font-medium">
              Entrar
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
