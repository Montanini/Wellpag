export interface AuthUser {
  token: string;
  nome: string;
  email: string;
  role: "PROFESSOR" | "ALUNO";
}

export function saveAuth(user: AuthUser) {
  localStorage.setItem("wellpag_token", user.token);
  localStorage.setItem("wellpag_user", JSON.stringify(user));
  // Cookie lido pelo middleware (server-side)
  document.cookie = `wellpag_token=${user.token}; path=/; max-age=86400; SameSite=Lax`;
}

export function getUser(): AuthUser | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem("wellpag_user");
  return raw ? JSON.parse(raw) : null;
}

export function logout() {
  localStorage.removeItem("wellpag_token");
  localStorage.removeItem("wellpag_user");
  document.cookie = "wellpag_token=; path=/; max-age=0";
  window.location.href = "/login";
}

export function isAuthenticated(): boolean {
  return !!localStorage.getItem("wellpag_token");
}

export function homeByRole(role: "PROFESSOR" | "ALUNO"): string {
  return role === "ALUNO" ? "/portal" : "/dashboard";
}
