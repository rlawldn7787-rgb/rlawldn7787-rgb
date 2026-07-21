/**
 * API base URL.
 * - Railway 통합 배포: NEXT_PUBLIC_API_URL 비우기 → 같은 도메인(/auth 등)으로 호출
 * - 로컬에서 API만 4000으로 띄울 때: NEXT_PUBLIC_API_URL=http://localhost:4000
 * - 빌드에 localhost가 잘못 박혀 있어도 브라우저에서는 무시하고 같은 도메인 사용
 */
export function getApiUrl() {
  const raw = (process.env.NEXT_PUBLIC_API_URL || "").replace(/\/$/, "");
  if (!raw || /localhost|127\.0\.0\.1/.test(raw)) {
    return "";
  }
  return raw;
}

export const API_URL = getApiUrl();

export type User = {
  id: number;
  username: string;
  name: string;
  role: "worker" | "admin";
};

export type RecordItem = {
  id: number;
  userId: number;
  workName: string;
  workType: string;
  location: string;
  content: string;
  workDate: string;
  photoUrl: string;
  photoThumbUrl: string | null;
  authorName: string;
  authorUsername: string;
  createdAt: string;
};

function authHeaders(token?: string | null): HeadersInit {
  const headers: Record<string, string> = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

function apiPath(path: string) {
  const base = getApiUrl();
  const normalized = path.startsWith("/") ? path : `/${path}`;
  return `${base}/api${normalized}`;
}

async function parseError(res: Response) {
  try {
    const data = await res.json();
    return data.error || "요청에 실패했습니다.";
  } catch {
    return "요청에 실패했습니다.";
  }
}

export async function login(username: string, password: string) {
  const url = apiPath("/auth/login");
  let res: Response;
  try {
    res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });
  } catch {
    const shown =
      typeof window !== "undefined" ? window.location.origin : url || "(same origin)";
    throw new Error(
      `서버에 연결할 수 없습니다. API(${shown})에 연결하지 못했습니다.`
    );
  }
  if (!res.ok) throw new Error(await parseError(res));
  return res.json() as Promise<{ token: string; user: User }>;
}

export async function fetchMe(token: string) {
  const res = await fetch(apiPath("/auth/me"), {
    headers: authHeaders(token),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json() as Promise<{ user: User }>;
}

export async function fetchRecords(
  token: string,
  params: Record<string, string>
) {
  const qs = new URLSearchParams(params).toString();
  const res = await fetch(apiPath(`/records${qs ? `?${qs}` : ""}`), {
    headers: authHeaders(token),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json() as Promise<{ records: RecordItem[] }>;
}

export async function fetchRecord(token: string, id: number) {
  const res = await fetch(apiPath(`/records/${id}`), {
    headers: authHeaders(token),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json() as Promise<{ record: RecordItem }>;
}

export async function downloadExcel(
  token: string,
  params: Record<string, string>
) {
  const qs = new URLSearchParams(params).toString();
  const res = await fetch(
    apiPath(`/records/export.xlsx${qs ? `?${qs}` : ""}`),
    { headers: authHeaders(token) }
  );
  if (!res.ok) throw new Error(await parseError(res));
  return res.blob();
}

export async function fetchUsers(token: string) {
  const res = await fetch(apiPath("/admin/users"), {
    headers: authHeaders(token),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json() as Promise<{
    users: Array<{
      id: number;
      username: string;
      name: string;
      role: "worker" | "admin";
      active: boolean;
      createdAt: string;
    }>;
  }>;
}

export async function createUser(
  token: string,
  body: {
    username: string;
    password: string;
    name: string;
    role: "worker" | "admin";
  }
) {
  const res = await fetch(apiPath("/admin/users"), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}

export async function patchUser(
  token: string,
  id: number,
  body: { active?: boolean; name?: string; password?: string; role?: string }
) {
  const res = await fetch(apiPath(`/admin/users/${id}`), {
    method: "PATCH",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}
