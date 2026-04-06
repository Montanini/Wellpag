"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { logout, getUser } from "@/lib/auth";
import { api } from "@/lib/api";

const links = [
  { href: "/dashboard",     label: "Dashboard"    },
  { href: "/alunos",        label: "Alunos"       },
  { href: "/horarios",      label: "Horários"     },
  { href: "/relatorios",    label: "Relatórios"   },
  { href: "/notificacoes",  label: "Notificações" },
  { href: "/whatsapp",      label: "WhatsApp"     },
];

export function Navbar() {
  const pathname = usePathname();
  const user = getUser();
  const [pendentes, setPendentes] = useState(0);

  useEffect(() => {
    api.get<{ id: string; status: string }[]>("/professor/notificacoes?status=PENDENTE")
      .then((lista) => setPendentes(lista.length))
      .catch(() => {});
  }, [pathname]);

  return (
    <nav className="bg-white border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          <div className="flex items-center gap-8">
            <span className="text-brand-600 font-bold text-xl">Wellpag</span>
            <div className="hidden sm:flex gap-1">
              {links.map(({ href, label }) => (
                <Link
                  key={href}
                  href={href}
                  className={`relative px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    pathname.startsWith(href)
                      ? "bg-brand-50 text-brand-700"
                      : "text-gray-600 hover:bg-gray-100"
                  }`}
                >
                  {label}
                  {href === "/notificacoes" && pendentes > 0 && (
                    <span className="absolute -top-0.5 -right-0.5 w-4 h-4 bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center">
                      {pendentes > 9 ? "9+" : pendentes}
                    </span>
                  )}
                </Link>
              ))}
            </div>
          </div>

          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-500 hidden sm:block">{user?.nome}</span>
            <button
              onClick={logout}
              className="text-sm text-gray-500 hover:text-gray-800 transition-colors"
            >
              Sair
            </button>
          </div>
        </div>

        {/* Mobile nav */}
        <div className="sm:hidden flex gap-1 pb-2 overflow-x-auto">
          {links.map(({ href, label }) => (
            <Link
              key={href}
              href={href}
              className={`relative px-3 py-1.5 rounded-md text-sm font-medium whitespace-nowrap transition-colors ${
                pathname.startsWith(href)
                  ? "bg-brand-50 text-brand-700"
                  : "text-gray-600 hover:bg-gray-100"
              }`}
            >
              {label}
              {href === "/notificacoes" && pendentes > 0 && (
                <span className="absolute -top-0.5 -right-0.5 w-3.5 h-3.5 bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center">
                  {pendentes}
                </span>
              )}
            </Link>
          ))}
        </div>
      </div>
    </nav>
  );
}
