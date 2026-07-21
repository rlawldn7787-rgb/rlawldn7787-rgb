import "dotenv/config";
import fs from "fs";
import path from "path";
import { Pool } from "pg";

type QueryResult = { rows: any[]; rowCount: number | null };

type DbClient = {
  query: (text: string, params?: unknown[]) => Promise<QueryResult>;
};

let poolImpl: DbClient;

function createPgPool(): DbClient {
  const connectionString =
    process.env.DATABASE_URL ||
    "postgresql://postgres:postgres@localhost:5432/woohaeng_board";

  const pgPool = new Pool({
    connectionString,
    ssl:
      process.env.DATABASE_SSL === "true"
        ? { rejectUnauthorized: false }
        : undefined,
  });

  return {
    async query(text, params) {
      const result = await pgPool.query(text, params);
      return { rows: result.rows, rowCount: result.rowCount };
    },
  };
}

async function createPGlite(): Promise<DbClient> {
  const { PGlite } = await import("@electric-sql/pglite");
  const dataDir = path.resolve(process.cwd(), "data", "pglite");
  fs.mkdirSync(dataDir, { recursive: true });
  const client = new PGlite(dataDir);

  return {
    async query(text, params) {
      const result = await client.query(text, params as never);
      return {
        rows: result.rows as any[],
        rowCount: (result as { affectedRows?: number }).affectedRows ?? result.rows.length,
      };
    },
  };
}

export const pool: DbClient = {
  query(text, params) {
    if (!poolImpl) {
      throw new Error("Database not initialized. Call initDb() first.");
    }
    return poolImpl.query(text, params);
  },
};

export async function initDb() {
  const preferPglite =
    process.env.USE_PGLITE === "1" ||
    process.env.DATABASE_URL?.startsWith("pglite:");

  if (preferPglite) {
    poolImpl = await createPGlite();
    console.log("Database: PGlite (local file)");
  } else {
    try {
      const pg = createPgPool();
      await pg.query("SELECT 1");
      poolImpl = pg;
      console.log("Database: PostgreSQL");
    } catch (err) {
      console.warn(
        "PostgreSQL 연결 실패, 로컬 PGlite로 전환합니다.",
        err instanceof Error ? err.message : err
      );
      poolImpl = await createPGlite();
      console.log("Database: PGlite (local file)");
    }
  }

  await pool.query(`
    CREATE TABLE IF NOT EXISTS users (
      id SERIAL PRIMARY KEY,
      username TEXT NOT NULL UNIQUE,
      password_hash TEXT NOT NULL,
      name TEXT NOT NULL,
      role TEXT NOT NULL CHECK (role IN ('worker', 'admin')),
      active BOOLEAN NOT NULL DEFAULT TRUE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    )
  `);

  await pool.query(`
    CREATE TABLE IF NOT EXISTS records (
      id SERIAL PRIMARY KEY,
      user_id INTEGER NOT NULL REFERENCES users(id),
      work_name TEXT NOT NULL,
      work_type TEXT NOT NULL DEFAULT '',
      location TEXT NOT NULL DEFAULT '',
      content TEXT NOT NULL DEFAULT '',
      work_date DATE NOT NULL,
      photo_key TEXT NOT NULL,
      photo_thumb_key TEXT,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    )
  `);

  await pool.query(
    `CREATE INDEX IF NOT EXISTS idx_records_work_date ON records(work_date)`
  );
  await pool.query(
    `CREATE INDEX IF NOT EXISTS idx_records_work_name ON records(work_name)`
  );
  await pool.query(
    `CREATE INDEX IF NOT EXISTS idx_records_user_id ON records(user_id)`
  );
}
