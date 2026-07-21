import { hashPassword } from "./auth";
import { pool } from "./db";

export async function seedAdmin() {
  const username = process.env.ADMIN_USERNAME || "admin";
  const password = process.env.ADMIN_PASSWORD || "admin1234";
  const name = process.env.ADMIN_NAME || "관리자";

  const existing = await pool.query(
    `SELECT id FROM users WHERE username = $1 LIMIT 1`,
    [username]
  );
  if ((existing.rowCount ?? existing.rows.length) > 0) {
    return;
  }

  const passwordHash = await hashPassword(password);
  try {
    await pool.query(
      `INSERT INTO users (username, password_hash, name, role, active)
       VALUES ($1, $2, $3, 'admin', TRUE)
       ON CONFLICT (username) DO NOTHING`,
      [username, passwordHash, name]
    );
    console.log(`Seeded admin user: ${username}`);
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    if (message.includes("duplicate") || message.includes("unique")) {
      return;
    }
    throw err;
  }
}
