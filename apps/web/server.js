/**
 * Railway 단일 서비스: Express API + Next.js 웹을 같은 PORT에서 제공합니다.
 * API 경로(/auth, /records, /admin, /uploads, /health)를 먼저 처리하고
 * 나머지는 Next.js로 넘깁니다.
 */
const path = require("path");
const next = require("next");
const { createApp } = require("@woohaeng/api/app");

async function main() {
  const port = Number(process.env.PORT || 3000);
  const dev = process.env.NODE_ENV !== "production";
  const webDir = __dirname;

  const nextApp = next({ dev, dir: webDir });
  const handle = nextApp.getRequestHandler();
  await nextApp.prepare();

  const server = await createApp();

  server.all("*", (req, res) => handle(req, res));

  server.listen(port, () => {
    console.log(`Combined web+api listening on :${port}`);
  });
}

main().catch((err) => {
  console.error("Failed to start combined server", err);
  process.exit(1);
});
