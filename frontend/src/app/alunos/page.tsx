"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Navbar } from "@/components/Navbar";
import { api } from "@/lib/api";
import { Aluno } from "@/lib/types";
import { getUser } from "@/lib/auth";

export default function AlunosPage() {
  const [alunos, setAlunos] = useState<Aluno[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");
  const user = getUser();

  // Link de convite para o aluno se cadastrar
  const linkCadastro = typeof window !== "undefined"
    ? `${window.location.origin}/alunos/cadastro?professor=${user ? JSON.parse(atob(localStorage.getItem("wellpag_token")!.split(".")[1])).sub : ""}`
    : "";

  useEffect(() => {
    api.get<Aluno[]>("/professor/alunos")
      .then(setAlunos)
      .catch((err) => setErro(err.message))
      .finally(() => setLoading(false));
  }, []);

  async function copiarLink() {
    await navigator.clipboard.writeText(linkCadastro);
    alert("Link copiado!");
  }

  return (
    <div className="min-h-screen">
      <Navbar />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
          <h1 className="text-2xl font-bold">Alunos</h1>
          <button
            onClick={copiarLink}
            className="bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
          >
            Copiar link de cadastro
          </button>
        </div>

        {loading ? (
          <div className="text-center py-16 text-gray-400 text-sm">Carregando...</div>
        ) : erro ? (
          <div className="text-center py-16 text-red-500 text-sm">{erro}</div>
        ) : alunos.length === 0 ? (
          <div className="text-center py-16 text-gray-400 text-sm">
            Nenhum aluno cadastrado. Compartilhe o link de cadastro com seus alunos.
          </div>
        ) : (
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Nome</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600 hidden sm:table-cell">E-mail</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600 hidden md:table-cell">Mensalidade</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600 hidden md:table-cell">Vencimento</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {alunos.map((aluno) => (
                  <tr key={aluno.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 font-medium">{aluno.nome}</td>
                    <td className="px-4 py-3 text-gray-500 hidden sm:table-cell">{aluno.email}</td>
                    <td className="px-4 py-3 text-gray-500 hidden md:table-cell">
                      {aluno.valorMensalidade != null
                        ? `R$ ${aluno.valorMensalidade.toFixed(2)}`
                        : <span className="text-yellow-600 text-xs">Não definido</span>
                      }
                    </td>
                    <td className="px-4 py-3 text-gray-500 hidden md:table-cell">
                      {aluno.diaVencimento != null ? `Dia ${aluno.diaVencimento}` : "—"}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Link
                        href={`/alunos/${aluno.id}`}
                        className="text-brand-600 hover:underline text-xs"
                      >
                        Detalhes
                      </Link>
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
