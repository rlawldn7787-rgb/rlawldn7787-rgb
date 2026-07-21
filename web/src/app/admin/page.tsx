"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "@/components/AppShell";
import { createUser, fetchUsers, patchUser } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useRouter } from "next/navigation";

type AdminUser = {
  id: number;
  username: string;
  name: string;
  role: "worker" | "admin";
  active: boolean;
  createdAt: string;
};

export default function AdminPage() {
  const { token, user } = useAuth();
  const router = useRouter();
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [error, setError] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [role, setRole] = useState<"worker" | "admin">("worker");

  async function load() {
    if (!token) return;
    try {
      const data = await fetchUsers(token);
      setUsers(data.users);
      setError("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "조회 실패");
    }
  }

  useEffect(() => {
    if (user && user.role !== "admin") {
      router.replace("/records");
      return;
    }
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, user]);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    if (!token) return;
    try {
      await createUser(token, { username, password, name, role });
      setUsername("");
      setPassword("");
      setName("");
      setRole("worker");
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "생성 실패");
    }
  }

  async function toggleActive(u: AdminUser) {
    if (!token) return;
    await patchUser(token, u.id, { active: !u.active });
    await load();
  }

  return (
    <AppShell>
      <div className="grid">
        <section className="panel">
          <h2>직원 계정 생성</h2>
          <form className="stack" onSubmit={onCreate} style={{ marginTop: 12 }}>
            <label>
              아이디
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </label>
            <label>
              비밀번호
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={4}
              />
            </label>
            <label>
              이름
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </label>
            <label>
              권한
              <select
                value={role}
                onChange={(e) =>
                  setRole(e.target.value as "worker" | "admin")
                }
              >
                <option value="worker">직원</option>
                <option value="admin">관리자</option>
              </select>
            </label>
            <button className="btn primary" type="submit">
              계정 생성
            </button>
          </form>
        </section>

        <section className="panel">
          <h2>계정 목록</h2>
          {error ? <p className="error">{error}</p> : null}
          <table className="table">
            <thead>
              <tr>
                <th>아이디</th>
                <th>이름</th>
                <th>권한</th>
                <th>상태</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.username}</td>
                  <td>{u.name}</td>
                  <td>{u.role === "admin" ? "관리자" : "직원"}</td>
                  <td>{u.active ? "활성" : "비활성"}</td>
                  <td>
                    <button
                      className="btn"
                      type="button"
                      onClick={() => toggleActive(u)}
                    >
                      {u.active ? "비활성화" : "활성화"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>
    </AppShell>
  );
}
