"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { logout, getUser } from "@/lib/auth";

const links = [
  { href: "/portal",            label: "Início"       },
  { href: "/portal/horarios",   label: "Meus Horários"},
  { href: "/portal/historico",  label: "Histórico"    },
  { href: "/portal/relatorio",  label: "Financeiro"   },
];

export function PortalNavbar() {
  const pathname = usePathname();
  const user = getUser();

  return (
    <nav className="bg-white border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          <div className="flex items-center gap-8">
            <span className="text-brand-600 font-bold text-xl">Wellpag</span>
            <span className="text-xs text-gray-400 hidden sm:block">Portal do Aluno</span>
            <div className="hidden sm:flex gap-1">
              {links.map(({ href, label }) => (
                <Link
                  key={href}
                  href={href}
                  className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    pathname === href
                      ? "bg-brand-50 text-brand-700"
                      : "text-gray-600 hover:bg-gray-100"
                  }`}
                >
                  {label}
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
              className={`px-3 py-1.5 rounded-md text-sm font-medium whitespace-nowrap transition-colors ${
                pathname === href
                  ? "bg-brand-50 text-brand-700"
                  : "text-gray-600 hover:bg-gray-100"
              }`}
            >
              {label}
            </Link>
          ))}
        </div>
      </div>
    </nav>
  );
}
