"use client";

import { Suspense } from "react";
import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { saveAuth } from "@/lib/auth";

function AuthCallbackInner() {
  const router = useRouter();
  const params = useSearchParams();

  useEffect(() => {
    const token = params.get("token");
    if (!token) {
      router.push("/login");
      return;
    }

    // Decodifica o payload do JWT para extrair nome, email e role
    try {
      const payload = JSON.parse(atob(token.split(".")[1]));
      const user = {
        token,
        nome:  payload.nome  ?? "",
        email: payload.email ?? "",
        role:  payload.role  ?? "PROFESSOR",
      };
      saveAuth(user);
      router.push(user.role === "ALUNO" ? "/portal" : "/dashboard");
    } catch {
      router.push("/login");
    }
  }, [params, router]);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <p className="text-gray-500 text-sm">Autenticando...</p>
    </div>
  );
}

export default function AuthCallbackPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-gray-500 text-sm">Autenticando...</p>
      </div>
    }>
      <AuthCallbackInner />
    </Suspense>
  );
}
