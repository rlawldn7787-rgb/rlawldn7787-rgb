import cors from "cors";
import express, { type Express } from "express";
import { initDb } from "./db";
import { seedAdmin } from "./seed";
import { ensureLocalUploads, getLocalUploadsDir, hasR2 } from "./storage";
import { authRouter } from "./routes/auth";
import { recordsRouter } from "./routes/records";
import { adminRouter } from "./routes/admin";

/** Express 앱만 생성 (listen 없음). 웹과 합쳐 띄울 때 사용합니다. */
export async function createApp(): Promise<Express> {
  await initDb();
  await seedAdmin();
  await ensureLocalUploads();

  const app = express();
  const corsOrigin = process.env.CORS_ORIGIN || "*";

  app.use(
    cors({
      origin: corsOrigin === "*" ? true : corsOrigin.split(",").map((s) => s.trim()),
      credentials: true,
    })
  );

  // Next.js 요청 body를 건드리지 않도록 API 경로에만 JSON 파서 적용
  const json = express.json({ limit: "2mb" });

  if (!hasR2()) {
    app.use("/uploads", express.static(getLocalUploadsDir()));
  }

  app.get("/health", (_req, res) => {
    res.json({ ok: true, service: "woohaeng-board-api" });
  });

  app.use("/auth", json, authRouter);
  app.use("/records", json, recordsRouter);
  app.use("/admin", json, adminRouter);

  return app;
}
