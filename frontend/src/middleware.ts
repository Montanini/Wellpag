import { NextRequest, NextResponse } from "next/server";

const PUBLIC_PATHS = ["/login", "/cadastro", "/auth/callback", "/alunos/cadastro"];

function decodeRole(token: string): "PROFESSOR" | "ALUNO" | null {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return payload.role ?? null;
  } catch {
    return null;
  }
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  const isPublic = PUBLIC_PATHS.some((p) => pathname.startsWith(p));
  if (isPublic) return NextResponse.next();

  const token = request.cookies.get("wellpag_token")?.value;

  if (!token) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  const role = decodeRole(token);
  if (!role) return NextResponse.redirect(new URL("/login", request.url));

  // Aluno tentando acessar área do professor
  if (role === "ALUNO" && pathname.startsWith("/dashboard") ||
      role === "ALUNO" && pathname.startsWith("/alunos") ||
      role === "ALUNO" && pathname.startsWith("/horarios")) {
    return NextResponse.redirect(new URL("/portal", request.url));
  }

  // Professor tentando acessar portal do aluno
  if (role === "PROFESSOR" && pathname.startsWith("/portal")) {
    return NextResponse.redirect(new URL("/dashboard", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
