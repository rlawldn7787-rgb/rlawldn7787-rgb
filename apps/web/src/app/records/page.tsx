"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
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
const POLL_MS = 5000;

export default function RecordsPage() {
  const { token } = useAuth();
  const initial = nowParts();
  const [year, setYear] = useState(initial.year);
  const [month, setMonth] = useState(initial.month);
  const [workName, setWorkName] = useState("");
  const [records, setRecords] = useState<RecordItem[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const params = useMemo(() => {
    const { from, to } = monthRange(year, month);
    const p: Record<string, string> = { from, to };
    if (workName.trim()) p.workName = workName.trim();
    return p;
  }, [year, month, workName]);

  const load = useCallback(
    async (opts?: { silent?: boolean }) => {
      if (!token) return;
      const silent = Boolean(opts?.silent);
      if (!silent) {
        setLoading(true);
        setError("");
      }
      try {
        const data = await fetchRecords(token, params);
        setRecords(data.records);
      } catch (err) {
        if (!silent) {
          setError(err instanceof Error ? err.message : "조회 실패");
        }
      } finally {
        if (!silent) setLoading(false);
      }
    },
    [token, params]
  );

  // 연·월 변경 또는 최초 진입 시 조회 (공사명은 '조회' 버튼으로 적용)
  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, year, month]);

  // 새 사진 업로드 반영: 5초마다 조용히 새로고침 + 탭 다시 볼 때 갱신
  useEffect(() => {
    if (!token) return;

    const tick = () => {
      if (document.visibilityState === "visible") {
        void load({ silent: true });
      }
    };
    const id = window.setInterval(tick, POLL_MS);
    const onVisible = () => {
      if (document.visibilityState === "visible") {
        void load({ silent: true });
      }
    };
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      window.clearInterval(id);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [token, load]);

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
              {year}년 {month}월 기록을 조회하고 엑셀로 내려받으세요.
            </p>
          </div>
          {!loading ? (
            <span className="muted">{records.length}건</span>
          ) : null}
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
              placeholder="검색어 입력"
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
