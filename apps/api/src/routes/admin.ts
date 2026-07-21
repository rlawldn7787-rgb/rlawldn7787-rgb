import { Router } from "express";
import { z } from "zod";
import { hashPassword, requireAdmin, requireAuth } from "../auth";
import { pool } from "../db";

export const adminRouter = Router();

adminRouter.use(requireAuth, requireAdmin);

adminRouter.get("/users", async (_req, res) => {
  const result = await pool.query(
    `SELECT id, username, name, role, active, created_at
     FROM users
     ORDER BY id ASC`
  );
  return res.json({
    users: result.rows.map((u) => ({
      id: u.id,
      username: u.username,
      name: u.name,
      role: u.role,
      active: u.active,
      createdAt: u.created_at,
    })),
  });
});

const createUserSchema = z.object({
  username: z.string().min(2),
  password: z.string().min(4),
  name: z.string().min(1),
  role: z.enum(["worker", "admin"]).default("worker"),
});

adminRouter.post("/users", async (req, res) => {
  const parsed = createUserSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ error: "입력값이 올바르지 않습니다." });
  }

  const { username, password, name, role } = parsed.data;
  const passwordHash = await hashPassword(password);

  try {
    const result = await pool.query(
      `INSERT INTO users (username, password_hash, name, role, active)
       VALUES ($1, $2, $3, $4, TRUE)
       RETURNING id, username, name, role, active, created_at`,
      [username, passwordHash, name, role]
    );
    const u = result.rows[0];
    return res.status(201).json({
      user: {
        id: u.id,
        username: u.username,
        name: u.name,
        role: u.role,
        active: u.active,
        createdAt: u.created_at,
      },
    });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "";
    if (message.includes("unique") || message.includes("duplicate")) {
      return res.status(409).json({ error: "이미 존재하는 아이디입니다." });
    }
    console.error(err);
    return res.status(500).json({ error: "계정 생성에 실패했습니다." });
  }
});

adminRouter.patch("/users/:id", async (req, res) => {
  const id = Number(req.params.id);
  const { active, name, password, role } = req.body as {
    active?: boolean;
    name?: string;
    password?: string;
    role?: "worker" | "admin";
  };

  const fields: string[] = [];
  const params: unknown[] = [];

  if (typeof active === "boolean") {
    params.push(active);
    fields.push(`active = $${params.length}`);
  }
  if (typeof name === "string" && name.trim()) {
    params.push(name.trim());
    fields.push(`name = $${params.length}`);
  }
  if (role === "worker" || role === "admin") {
    params.push(role);
    fields.push(`role = $${params.length}`);
  }
  if (typeof password === "string" && password.length >= 4) {
    params.push(await hashPassword(password));
    fields.push(`password_hash = $${params.length}`);
  }

  if (!fields.length) {
    return res.status(400).json({ error: "변경할 항목이 없습니다." });
  }

  params.push(id);
  const result = await pool.query(
    `UPDATE users SET ${fields.join(", ")}
     WHERE id = $${params.length}
     RETURNING id, username, name, role, active, created_at`,
    params
  );

  if (!result.rows[0]) {
    return res.status(404).json({ error: "사용자를 찾을 수 없습니다." });
  }

  const u = result.rows[0];
  return res.json({
    user: {
      id: u.id,
      username: u.username,
      name: u.name,
      role: u.role,
      active: u.active,
      createdAt: u.created_at,
    },
  });
});
