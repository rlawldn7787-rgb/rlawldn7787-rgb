import ExcelJS from "exceljs";
import sharp from "sharp";
import { getObjectBuffer } from "./storage";

export interface ExportRow {
  work_date: string | Date;
  work_name: string;
  work_type: string;
  location: string;
  content: string;
  author_name: string;
  created_at: string | Date;
  photo_key: string;
}

function formatDate(value: string | Date) {
  const d = typeof value === "string" ? new Date(value) : value;
  if (Number.isNaN(d.getTime())) return String(value);
  return d.toISOString().slice(0, 10);
}

function formatDateTime(value: string | Date) {
  const d = typeof value === "string" ? new Date(value) : value;
  if (Number.isNaN(d.getTime())) return String(value);
  return d.toISOString().replace("T", " ").slice(0, 19);
}

export async function buildRecordsExcel(rows: ExportRow[]): Promise<Buffer> {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = "우행통신 보드판";
  const sheet = workbook.addWorksheet("현장기록");

  sheet.columns = [
    { header: "일자", key: "work_date", width: 12 },
    { header: "공사명", key: "work_name", width: 24 },
    { header: "공종", key: "work_type", width: 16 },
    { header: "위치", key: "location", width: 20 },
    { header: "내용", key: "content", width: 36 },
    { header: "작성자", key: "author_name", width: 12 },
    { header: "업로드시각", key: "created_at", width: 20 },
    { header: "사진", key: "photo", width: 28 },
  ];

  sheet.getRow(1).font = { bold: true };
  sheet.getRow(1).height = 22;

  const photoCol = 8; // 1-based column for images
  const thumbWidth = 160;
  const thumbHeight = 120;

  for (let i = 0; i < rows.length; i++) {
    const row = rows[i];
    const excelRow = sheet.addRow({
      work_date: formatDate(row.work_date),
      work_name: row.work_name,
      work_type: row.work_type,
      location: row.location,
      content: row.content,
      author_name: row.author_name,
      created_at: formatDateTime(row.created_at),
      photo: "",
    });
    excelRow.height = 95;

    try {
      const raw = await getObjectBuffer(row.photo_key);
      if (!raw) continue;
      const thumb = await sharp(raw)
        .rotate()
        .resize({
          width: thumbWidth * 2,
          height: thumbHeight * 2,
          fit: "cover",
        })
        .jpeg({ quality: 80 })
        .toBuffer();

      const imageId = workbook.addImage({
        buffer: Uint8Array.from(thumb) as unknown as ExcelJS.Buffer,
        extension: "jpeg",
      });

      // ExcelJS uses 0-based row/col for image anchors
      sheet.addImage(imageId, {
        tl: { col: photoCol - 1, row: excelRow.number - 1 },
        ext: { width: thumbWidth, height: thumbHeight },
        editAs: "oneCell",
      });
    } catch (err) {
      console.warn("Failed to embed photo for row", excelRow.number, err);
      excelRow.getCell(photoCol).value = "(사진 없음)";
    }
  }

  const arrayBuffer = await workbook.xlsx.writeBuffer();
  return Buffer.from(arrayBuffer);
}
