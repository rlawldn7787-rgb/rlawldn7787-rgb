"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/AppShell";
import { deleteRecord, fetchRecord, RecordItem } from "@/lib/api";
import { useAuth } from "@/lib/auth";

export default function RecordDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { token, user } = useAuth();
  const [record, setRecord] = useState<RecordItem | null>(null);
  const [error, setError] = useState("");
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!token || !params.id) return;
    fetchRecord(token, Number(params.id))
      .then((data) => setRecord(data.record))
      .catch((err) =>
        setError(err instanceof Error ? err.message : "조회 실패")
      );
  }, [token, params.id]);

  async function onDelete() {
    if (!token || !record) return;
    const ok = window.confirm(
      `"${record.workName}" 기록을 삭제할까요?\n이 작업은 되돌릴 수 없습니다.`
    );
    if (!ok) return;
    setDeleting(true);
    setError("");
    try {
      await deleteRecord(token, record.id);
      router.replace("/records");
    } catch (err) {
      setError(err instanceof Error ? err.message : "삭제 실패");
      setDeleting(false);
    }
  }

  return (
    <AppShell>
      <section className="panel detail">
        <div className="actions">
          <Link className="btn" href="/records">
            목록으로
          </Link>
          {user?.role === "admin" ? (
            <button
              className="btn danger"
              type="button"
              disabled={deleting || !record}
              onClick={onDelete}
            >
              {deleting ? "삭제 중..." : "기록 삭제"}
            </button>
          ) : null}
        </div>
        {error ? <p className="error">{error}</p> : null}
        {!record && !error ? <p className="muted">불러오는 중...</p> : null}
        {record ? (
          <>
            <img src={record.photoUrl} alt={record.workName} />
            <div className="detail-meta">
              <strong>{record.workName}</strong>
              <div className="meta-row">
                <span>일자</span>
                <span>{String(record.workDate).slice(0, 10)}</span>
              </div>
              <div className="meta-row">
                <span>공종</span>
                <span>{record.workType || "-"}</span>
              </div>
              <div className="meta-row">
                <span>위치</span>
                <span>{record.location || "-"}</span>
              </div>
              <div className="meta-row">
                <span>내용</span>
                <span>{record.content || "-"}</span>
              </div>
              <div className="meta-row">
                <span>업로드</span>
                <span>
                  {new Date(record.createdAt).toLocaleString("ko-KR")}
                </span>
              </div>
            </div>
          </>
        ) : null}
      </section>
    </AppShell>
  );
}
