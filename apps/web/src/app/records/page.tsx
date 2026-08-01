"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import { AppShell } from "@/components/AppShell";
import { downloadExcel, fetchRecords, RecordItem } from "@/lib/api";
import { useAuth } from "@/lib/auth";

function nowParts() {
  const d = new Date();
  return { year: d.getFullYear(), month: d.getMonth() + 1 };
}

function monthRange(year: number, month: number) {
  const from = `${year}-${String(month).padStart(2, "0")}-01`;
  const lastDay = new Date(year, month, 0).getDate();
  const to = `${year}-${String(month).padStart(2, "0")}-${String(lastDay).padStart(2, "0")}`;
  return { from, to };
}

const YEAR_OPTIONS = (() => {
  const current = new Date().getFullYear();
  const years: number[] = [];
  for (let y = current + 1; y >= current - 6; y -= 1) years.push(y);
  return years;
})();

const MONTH_OPTIONS = Array.from({ length: 12 }, (_, i) => i + 1);
const POLL_MS = 3000;

export default function RecordsPage() {
  const { token } = useAuth();
  const initial = nowParts();
  const [year, setYear] = useState(initial.year);
  const [month, setMonth] = useState(initial.month);
  const [workName, setWorkName] = useState("");
  const [appliedWorkName, setAppliedWorkName] = useState("");
  const [records, setRecords] = useState<RecordItem[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [lastSyncedAt, setLastSyncedAt] = useState<Date | null>(null);

  const params = useMemo(() => {
    const { from, to } = monthRange(year, month);
    const p: Record<string, string> = { from, to };
    if (appliedWorkName.trim()) p.workName = appliedWorkName.trim();
    return p;
  }, [year, month, appliedWorkName]);

  const tokenRef = useRef(token);
  const paramsRef = useRef(params);
  tokenRef.current = token;
  paramsRef.current = params;

  useEffect(() => {
    if (!token) return;

    let cancelled = false;

    async function refresh(silent: boolean) {
      const t = tokenRef.current;
      if (!t || cancelled) return;
      if (!silent) {
        setLoading(true);
        setError("");
      }
      try {
        const data = await fetchRecords(t, paramsRef.current);
        if (cancelled) return;
        setRecords(data.records);
        setLastSyncedAt(new Date());
      } catch (err) {
        if (!silent && !cancelled) {
          setError(err instanceof Error ? err.message : "조회 실패");
        }
      } finally {
        if (!silent && !cancelled) setLoading(false);
      }
    }

    void refresh(false);

    const id = window.setInterval(() => {
      if (document.visibilityState === "visible") {
        void refresh(true);
      }
    }, POLL_MS);

    const onVisible = () => {
      if (document.visibilityState === "visible") {
        void refresh(true);
      }
    };
    document.addEventListener("visibilitychange", onVisible);

    return () => {
      cancelled = true;
      window.clearInterval(id);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [token, year, month, appliedWorkName]);

  function onSearch() {
    setAppliedWorkName(workName.trim());
  }

  async function onExport() {
    if (!token) return;
    try {
      const blob = await downloadExcel(token, params);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `우행통신_기록_${year}-${String(month).padStart(2, "0")}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : "엑셀 다운로드 실패");
    }
  }

  return (
    <AppShell>
      <section className="panel">
        <div className="panel-head">
          <div>
            <h2>현장 기록</h2>
            <p>
              {year}년 {month}월 기록 · 새 사진은 자동으로 반영됩니다.
            </p>
          </div>
          <div style={{ textAlign: "right" }}>
            {!loading ? (
              <span className="muted">{records.length}건</span>
            ) : null}
            {lastSyncedAt ? (
              <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
                자동갱신{" "}
                {lastSyncedAt.toLocaleTimeString("ko-KR", {
                  hour: "2-digit",
                  minute: "2-digit",
                  second: "2-digit",
                })}
              </div>
            ) : null}
          </div>
        </div>

        <div className="filters">
          <label>
            연도
            <select
              value={year}
              onChange={(e) => setYear(Number(e.target.value))}
            >
              {YEAR_OPTIONS.map((y) => (
                <option key={y} value={y}>
                  {y}년
                </option>
              ))}
            </select>
          </label>
          <label>
            월
            <select
              value={month}
              onChange={(e) => setMonth(Number(e.target.value))}
            >
              {MONTH_OPTIONS.map((m) => (
                <option key={m} value={m}>
                  {m}월
                </option>
              ))}
            </select>
          </label>
          <label>
            공사명
            <input
              value={workName}
              onChange={(e) => setWorkName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") onSearch();
              }}
              placeholder="검색어 입력"
            />
          </label>
          <div className="actions">
            <button className="btn primary" type="button" onClick={onSearch}>
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
          <div className="empty-state">
            {year}년 {month}월 조건에 맞는 기록이 없습니다.
          </div>
        ) : null}

        <div className="records">
          {records.map((r) => (
            <Link key={r.id} href={`/records/${r.id}`} className="card">
              <img src={r.photoThumbUrl || r.photoUrl} alt={r.workName} />
              <div className="body">
                <strong>{r.workName}</strong>
                <div className="meta">
                  {String(r.workDate).slice(0, 10)}
                </div>
                {r.workType ? <span className="tag">{r.workType}</span> : null}
                <div className="meta">{r.location || "위치 미입력"}</div>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </AppShell>
  );
}
