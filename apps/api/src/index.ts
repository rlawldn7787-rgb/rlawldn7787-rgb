import dotenv from "dotenv";
import path from "path";
dotenv.config({ path: path.resolve(__dirname, "../.env") });
import { createApp } from "./app";
import { hasR2 } from "./storage";

async function main() {
  const app = await createApp();
  const port = Number(process.env.PORT || 4000);

  app.listen(port, () => {
    console.log(`API listening on :${port}`);
    console.log(`Storage mode: ${hasR2() ? "Cloudflare R2" : "local uploads"}`);
  });
}

main().catch((err) => {
  console.error("Failed to start API", err);
  process.exit(1);
});
