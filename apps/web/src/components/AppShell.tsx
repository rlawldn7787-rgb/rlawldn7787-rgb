"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/lib/auth";

export function AppShell({ children }: { children: React.ReactNode }) {
  const { token, user, loading, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!loading && !token) router.replace("/");
  }, [loading, token, router]);

  if (loading || !user) {
    return (
      <div className="shell">
        <p className="muted">불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <strong>우행통신 보드판</strong>
          <span>
            {user.name} ({user.role === "admin" ? "관리자" : "직원"})
          </span>
        </div>
        <nav className="nav">
          <Link
            href="/records"
            className={pathname.startsWith("/records") ? "active" : ""}
          >
            기록
          </Link>
          {user.role === "admin" ? (
            <Link
              href="/admin"
              className={pathname.startsWith("/admin") ? "active" : ""}
            >
              계정관리
            </Link>
          ) : null}
          <button type="button" onClick={logout}>
            로그아웃
          </button>
        </nav>
      </header>
      {children}
    </div>
  );
}
