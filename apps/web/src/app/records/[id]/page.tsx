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
            <div className="grid">
              <div>
                <strong>{record.workName}</strong>
              </div>
              <div className="meta">
                일자: {String(record.workDate).slice(0, 10)}
              </div>
              <div className="meta">공종: {record.workType || "-"}</div>
              <div className="meta">위치: {record.location || "-"}</div>
              <div className="meta">내용: {record.content || "-"}</div>
              <div className="meta">
                작성자: {record.authorName} ({record.authorUsername})
              </div>
              <div className="meta">
                업로드: {new Date(record.createdAt).toLocaleString("ko-KR")}
              </div>
            </div>
          </>
        ) : null}
      </section>
    </AppShell>
  );
}
