"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/AppShell";
import { downloadExcel, fetchRecords, RecordItem } from "@/lib/api";
import { useAuth } from "@/lib/auth";

function today() {
  return new Date().toISOString().slice(0, 10);
}

function monthStart() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-01`;
}

export default function RecordsPage() {
  const { token } = useAuth();
  const [from, setFrom] = useState(monthStart());
  const [to, setTo] = useState(today());
  const [workName, setWorkName] = useState("");
  const [records, setRecords] = useState<RecordItem[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const params = useMemo(() => {
    const p: Record<string, string> = {};
    if (from) p.from = from;
    if (to) p.to = to;
    if (workName.trim()) p.workName = workName.trim();
    return p;
  }, [from, to, workName]);

  async function load() {
    if (!token) return;
    setLoading(true);
    setError("");
    try {
      const data = await fetchRecords(token, params);
      setRecords(data.records);
    } catch (err) {
      setError(err instanceof Error ? err.message : "조회 실패");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  async function onExport() {
    if (!token) return;
    try {
      const blob = await downloadExcel(token, params);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `우행통신_기록_${from || "all"}_${to || "all"}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : "엑셀 다운로드 실패");
    }
  }

  return (
    <AppShell>
      <section className="panel">
        <div className="filters">
          <label>
            시작일
            <input
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            />
          </label>
          <label>
            종료일
            <input
              type="date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          </label>
          <label>
            공사명
            <input
              value={workName}
              onChange={(e) => setWorkName(e.target.value)}
              placeholder="검색"
            />
          </label>
          <div className="actions">
            <button className="btn primary" type="button" onClick={load}>
              조회
            </button>
            <button className="btn" type="button" onClick={onExport}>
              엑셀 다운로드
            </button>
          </div>
        </div>
        {error ? <p className="error">{error}</p> : null}
        {loading ? <p className="muted">불러오는 중...</p> : null}
        {!loading && records.length === 0 ? (
          <p className="muted">조건에 맞는 기록이 없습니다.</p>
        ) : null}
        <div className="records">
          {records.map((r) => (
            <Link key={r.id} href={`/records/${r.id}`} className="card">
              <img
                src={r.photoThumbUrl || r.photoUrl}
                alt={r.workName}
              />
              <div className="body">
                <strong>{r.workName}</strong>
                <div className="meta">
                  {String(r.workDate).slice(0, 10)} · {r.authorName}
                </div>
                <div className="meta">
                  {r.workType || "-"} / {r.location || "-"}
                </div>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </AppShell>
  );
}
