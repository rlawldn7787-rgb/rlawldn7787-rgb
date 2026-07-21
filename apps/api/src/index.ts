import dotenv from "dotenv";
import path from "path";
dotenv.config({ path: path.resolve(__dirname, "../.env") });
import cors from "cors";
import express from "express";
import { initDb } from "./db";
import { seedAdmin } from "./seed";
import { ensureLocalUploads, getLocalUploadsDir, hasR2 } from "./storage";
import { authRouter } from "./routes/auth";
import { recordsRouter } from "./routes/records";
import { adminRouter } from "./routes/admin";

async function main() {
  await initDb();
  await seedAdmin();
  await ensureLocalUploads();

  const app = express();
  const port = Number(process.env.PORT || 4000);
  const corsOrigin = process.env.CORS_ORIGIN || "*";

  app.use(
    cors({
      origin: corsOrigin === "*" ? true : corsOrigin.split(",").map((s) => s.trim()),
      credentials: true,
    })
  );
  app.use(express.json({ limit: "2mb" }));

  if (!hasR2()) {
    app.use("/uploads", express.static(getLocalUploadsDir()));
  }

  app.get("/health", (_req, res) => {
    res.json({ ok: true, service: "woohaeng-board-api" });
  });

  app.use("/auth", authRouter);
  app.use("/records", recordsRouter);
  app.use("/admin", adminRouter);

  app.listen(port, () => {
    console.log(`API listening on :${port}`);
    console.log(`Storage mode: ${hasR2() ? "Cloudflare R2" : "local uploads"}`);
  });
}

main().catch((err) => {
  console.error("Failed to start API", err);
  process.exit(1);
});
