"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { login } from "@/lib/api";
import { useAuth } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const { setSession, token, loading } = useAuth();
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!loading && token) router.replace("/records");
  }, [loading, token, router]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      const data = await login(username, password);
      setSession(data.token, data.user);
      router.push("/records");
    } catch (err) {
      setError(err instanceof Error ? err.message : "로그인 실패");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-wrap">
      <form className="panel login-card stack" onSubmit={onSubmit}>
        <div>
          <h1>우행통신 보드판</h1>
          <p>현장 기록 조회 · 엑셀 다운로드</p>
        </div>
        <label>
          아이디
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
          />
        </label>
        <label>
          비밀번호
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {error ? <div className="error">{error}</div> : null}
        <button className="btn primary" type="submit" disabled={submitting}>
          {submitting ? "로그인 중..." : "로그인"}
        </button>
      </form>
    </div>
  );
}
