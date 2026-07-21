/** 통합 배포 시 비워 두면 브라우저가 같은 주소로 API를 호출합니다. */
export function getApiUrl() {
  return (process.env.NEXT_PUBLIC_API_URL || "").replace(/\/$/, "");
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

async function parseError(res: Response) {
  try {
    const data = await res.json();
    return data.error || "요청에 실패했습니다.";
  } catch {
    return "요청에 실패했습니다.";
  }
}

export async function login(username: string, password: string) {
  let res: Response;
  try {
    res = await fetch(`${API_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });
  } catch {
    throw new Error(
      `서버에 연결할 수 없습니다. API(${API_URL})가 실행 중인지 확인하세요.`
    );
  }
  if (!res.ok) throw new Error(await parseError(res));
  return res.json() as Promise<{ token: string; user: User }>;
}

export async function fetchMe(token: string) {
  const res = await fetch(`${API_URL}/auth/me`, {
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
  const res = await fetch(`${API_URL}/records${qs ? `?${qs}` : ""}`, {
    headers: authHeaders(token),
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json() as Promise<{ records: RecordItem[] }>;
}

export async function fetchRecord(token: string, id: number) {
  const res = await fetch(`${API_URL}/records/${id}`, {
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
    `${API_URL}/records/export.xlsx${qs ? `?${qs}` : ""}`,
    { headers: authHeaders(token) }
  );
  if (!res.ok) throw new Error(await parseError(res));
  return res.blob();
}

export async function fetchUsers(token: string) {
  const res = await fetch(`${API_URL}/admin/users`, {
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
  const res = await fetch(`${API_URL}/admin/users`, {
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
  const res = await fetch(`${API_URL}/admin/users/${id}`, {
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
