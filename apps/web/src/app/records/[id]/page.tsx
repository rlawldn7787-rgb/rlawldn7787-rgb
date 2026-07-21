"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/AppShell";
import { fetchRecord, RecordItem } from "@/lib/api";
import { useAuth } from "@/lib/auth";

export default function RecordDetailPage() {
  const params = useParams<{ id: string }>();
  const { token } = useAuth();
  const [record, setRecord] = useState<RecordItem | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!token || !params.id) return;
    fetchRecord(token, Number(params.id))
      .then((data) => setRecord(data.record))
      .catch((err) =>
        setError(err instanceof Error ? err.message : "조회 실패")
      );
  }, [token, params.id]);

  return (
    <AppShell>
      <section className="panel detail">
        <div className="actions">
          <Link className="btn" href="/records">
            목록으로
          </Link>
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
