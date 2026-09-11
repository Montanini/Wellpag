"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { logout, getUser, AuthUser } from "@/lib/auth";

const links = [
  { href: "/dashboard",     label: "Dashboard"    },
  { href: "/alunos",        label: "Alunos"       },
  { href: "/horarios",      label: "Horários"     },
  { href: "/relatorios",    label: "Relatórios"   },
  { href: "/whatsapp",      label: "WhatsApp"     },
];

export function Navbar() {
  const pathname = usePathname();
  const [user, setUser] = useState<AuthUser | null>(null);

  useEffect(() => {
    // getUser() lê localStorage, que so existe no cliente — chamar aqui (e nao
    // direto no corpo do componente) evita divergencia entre o HTML renderizado
    // no servidor (sem usuario) e a primeira renderizacao no cliente
    // (hydration mismatch).
    setUser(getUser());
  }, []);

  return (
    <nav className="bg-white border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          <div className="flex items-center gap-8">
            <Link href="/dashboard">
              <Image src="/logo.png" alt="Wellpag" width={120} height={40} className="h-10 w-auto object-contain" priority />
            </Link>
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
            </Link>
          ))}
        </div>
      </div>
    </nav>
  );
}
