import { Router } from "express";
import multer from "multer";
import sharp from "sharp";
import { z } from "zod";
import { requireAuth } from "../auth";
import { pool } from "../db";
import { buildRecordsExcel } from "../excel";
import { publicUrlForKey, uploadBuffer } from "../storage";

export const recordsRouter = Router();
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 20 * 1024 * 1024 },
});

const createSchema = z.object({
  workName: z.string().min(1),
  workType: z.string().optional().default(""),
  location: z.string().optional().default(""),
  content: z.string().optional().default(""),
  workDate: z.string().min(1),
});

function mapRecord(row: Record<string, unknown>) {
  return {
    id: row.id,
    userId: row.user_id,
    workName: row.work_name,
    workType: row.work_type,
    location: row.location,
    content: row.content,
    workDate: row.work_date,
    photoUrl: publicUrlForKey(String(row.photo_key)),
    photoThumbUrl: row.photo_thumb_key
      ? publicUrlForKey(String(row.photo_thumb_key))
      : null,
    authorName: row.author_name,
    authorUsername: row.author_username,
    createdAt: row.created_at,
  };
}

function buildFilter(query: {
  from?: string;
  to?: string;
  workName?: string;
  siteName?: string;
  authorId?: string;
  mine?: string;
}, userId?: number) {
  const where: string[] = [];
  const params: unknown[] = [];

  const workName = query.workName || query.siteName;
  if (query.from) {
    params.push(query.from);
    where.push(`r.work_date >= $${params.length}`);
  }
  if (query.to) {
    params.push(query.to);
    where.push(`r.work_date <= $${params.length}`);
  }
  if (workName) {
    params.push(`%${workName}%`);
    where.push(`r.work_name ILIKE $${params.length}`);
  }
  if (query.authorId) {
    params.push(Number(query.authorId));
    where.push(`r.user_id = $${params.length}`);
  }
  if (query.mine === "1" && userId) {
    params.push(userId);
    where.push(`r.user_id = $${params.length}`);
  }

  return {
    clause: where.length ? `WHERE ${where.join(" AND ")}` : "",
    params,
  };
}

recordsRouter.get("/", requireAuth, async (req, res) => {
  const { clause, params } = buildFilter(req.query as never, req.user!.id);
  const result = await pool.query(
    `SELECT r.*, u.name AS author_name, u.username AS author_username
     FROM records r
     JOIN users u ON u.id = r.user_id
     ${clause}
     ORDER BY r.work_date DESC, r.created_at DESC
     LIMIT 500`,
    params
  );
  return res.json({ records: result.rows.map(mapRecord) });
});

recordsRouter.get("/export.xlsx", requireAuth, async (req, res) => {
  const { clause, params } = buildFilter(req.query as never, req.user!.id);
  const result = await pool.query(
    `SELECT r.work_date, r.work_name, r.work_type, r.location, r.content,
            r.photo_key, r.created_at, u.name AS author_name
     FROM records r
     JOIN users u ON u.id = r.user_id
     ${clause}
     ORDER BY r.work_date DESC, r.created_at DESC`,
    params
  );

  const buffer = await buildRecordsExcel(result.rows);
  res.setHeader(
    "Content-Type",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  );
  res.setHeader(
    "Content-Disposition",
    `attachment; filename="woohaeng-records.xlsx"`
  );
  return res.send(buffer);
});

recordsRouter.get("/:id", requireAuth, async (req, res) => {
  const result = await pool.query(
    `SELECT r.*, u.name AS author_name, u.username AS author_username
     FROM records r
     JOIN users u ON u.id = r.user_id
     WHERE r.id = $1`,
    [Number(req.params.id)]
  );
  if (!result.rows[0]) {
    return res.status(404).json({ error: "기록을 찾을 수 없습니다." });
  }
  return res.json({ record: mapRecord(result.rows[0]) });
});

recordsRouter.post(
  "/",
  requireAuth,
  upload.single("image"),
  async (req, res) => {
    try {
      const parsed = createSchema.safeParse(req.body);
      if (!parsed.success) {
        return res.status(400).json({ error: "필수 항목이 누락되었습니다." });
      }
      if (!req.file) {
        return res.status(400).json({ error: "사진 파일이 필요합니다." });
      }

      const imageBuffer = req.file.buffer;
      const photoKey = await uploadBuffer(imageBuffer, "jpg", "image/jpeg");

      let thumbKey: string | null = null;
      try {
        const thumb = await sharp(imageBuffer)
          .rotate()
          .resize({ width: 480, withoutEnlargement: true })
          .jpeg({ quality: 75 })
          .toBuffer();
        thumbKey = await uploadBuffer(thumb, "jpg", "image/jpeg");
      } catch {
        thumbKey = null;
      }

      const { workName, workType, location, content, workDate } = parsed.data;
      const insert = await pool.query(
        `INSERT INTO records
          (user_id, work_name, work_type, location, content, work_date, photo_key, photo_thumb_key)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
         RETURNING *`,
        [
          req.user!.id,
          workName,
          workType || "",
          location || "",
          content || "",
          workDate,
          photoKey,
          thumbKey,
        ]
      );

      const row = {
        ...insert.rows[0],
        author_name: req.user!.name,
        author_username: req.user!.username,
      };

      return res.status(201).json({ record: mapRecord(row) });
    } catch (err) {
      console.error(err);
      return res.status(500).json({ error: "업로드에 실패했습니다." });
    }
  }
);
