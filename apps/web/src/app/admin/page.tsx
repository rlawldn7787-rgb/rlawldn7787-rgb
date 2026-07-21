"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "@/components/AppShell";
import {
  createUser,
  deleteWorkType,
  fetchUsers,
  fetchWorkTypes,
  patchUser,
} from "@/lib/api";
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

type WorkTypeRow = {
  name: string;
  count: number;
};

export default function AdminPage() {
  const { token, user } = useAuth();
  const router = useRouter();
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [workTypes, setWorkTypes] = useState<WorkTypeRow[]>([]);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [role, setRole] = useState<"worker" | "admin">("worker");
  const [deleting, setDeleting] = useState<string | null>(null);

  async function load() {
    if (!token) return;
    try {
      const [userData, workTypeData] = await Promise.all([
        fetchUsers(token),
        fetchWorkTypes(token),
      ]);
      setUsers(userData.users);
      setWorkTypes(workTypeData.workTypes);
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
      setMessage("계정을 생성했습니다.");
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

  async function onDeleteWorkType(wt: WorkTypeRow) {
    if (!token) return;
    const ok = window.confirm(
      `"${wt.name}" 공종으로 등록된 기록 ${wt.count}건을 모두 삭제할까요?\n이 작업은 되돌릴 수 없습니다.`
    );
    if (!ok) return;
    setDeleting(wt.name);
    setMessage("");
    setError("");
    try {
      const result = await deleteWorkType(token, wt.name);
      setMessage(`"${result.name}" 공종 기록 ${result.deleted}건을 삭제했습니다.`);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "공종 삭제 실패");
    } finally {
      setDeleting(null);
    }
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
          {message ? <p className="muted">{message}</p> : null}
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

        <section className="panel" style={{ gridColumn: "1 / -1" }}>
          <h2>공종 관리</h2>
          <p className="muted" style={{ marginTop: 8 }}>
            기록에 사용된 공종 목록입니다. 삭제하면 해당 공종으로 올라온 사진
            기록이 모두 지워집니다.
          </p>
          {workTypes.length === 0 ? (
            <p className="muted" style={{ marginTop: 12 }}>
              등록된 공종이 없습니다.
            </p>
          ) : (
            <table className="table" style={{ marginTop: 12 }}>
              <thead>
                <tr>
                  <th>공종</th>
                  <th>기록 수</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {workTypes.map((wt) => (
                  <tr key={wt.name}>
                    <td>{wt.name}</td>
                    <td>{wt.count}건</td>
                    <td>
                      <button
                        className="btn danger"
                        type="button"
                        disabled={deleting === wt.name}
                        onClick={() => onDeleteWorkType(wt)}
                      >
                        {deleting === wt.name ? "삭제 중..." : "삭제하기"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </AppShell>
  );
}
