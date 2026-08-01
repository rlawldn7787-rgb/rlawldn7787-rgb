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

export interface ExportOptions {
  from?: string;
  to?: string;
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

const thinBorder: Partial<ExcelJS.Borders> = {
  top: { style: "thin", color: { argb: "FF222222" } },
  left: { style: "thin", color: { argb: "FF222222" } },
  bottom: { style: "thin", color: { argb: "FF222222" } },
  right: { style: "thin", color: { argb: "FF222222" } },
};

const centerAlign: Partial<ExcelJS.Alignment> = {
  horizontal: "center",
  vertical: "middle",
  wrapText: true,
};

function styleCell(
  cell: ExcelJS.Cell,
  opts: {
    bold?: boolean;
    size?: number;
    fill?: string;
    color?: string;
  } = {}
) {
  cell.alignment = { ...centerAlign };
  cell.border = thinBorder;
  cell.font = {
    name: "맑은 고딕",
    size: opts.size ?? 11,
    bold: opts.bold ?? false,
    color: opts.color ? { argb: opts.color } : { argb: "FF000000" },
  };
  if (opts.fill) {
    cell.fill = {
      type: "pattern",
      pattern: "solid",
      fgColor: { argb: opts.fill },
    };
  }
}

export async function buildRecordsExcel(
  rows: ExportRow[],
  options: ExportOptions = {}
): Promise<Buffer> {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = "우행통신 보드판";
  const sheet = workbook.addWorksheet("현장기록", {
    views: [{ state: "frozen", ySplit: 3 }],
  });

  const colCount = 6;
  sheet.columns = [
    { key: "work_date", width: 12 },
    { key: "work_name", width: 22 },
    { key: "work_type", width: 14 },
    { key: "location", width: 18 },
    { key: "content", width: 32 },
    { key: "photo", width: 26 },
  ];

  // 1행: 제목 (점검표 느낌)
  sheet.mergeCells(1, 1, 1, colCount);
  const titleCell = sheet.getCell(1, 1);
  titleCell.value = "우행통신 현장 보드판 기록";
  styleCell(titleCell, {
    bold: true,
    size: 16,
    fill: "FF0B1F3A",
    color: "FFFFFFFF",
  });
  for (let c = 2; c <= colCount; c++) {
    styleCell(sheet.getCell(1, c), {
      bold: true,
      size: 16,
      fill: "FF0B1F3A",
      color: "FFFFFFFF",
    });
  }
  sheet.getRow(1).height = 32;

  // 2행: 기간
  sheet.mergeCells(2, 1, 2, colCount);
  const period =
    options.from || options.to
      ? `조회기간: ${options.from || "전체"} ~ ${options.to || "전체"}`
      : `조회기간: 전체 · 총 ${rows.length}건`;
  const periodCell = sheet.getCell(2, 1);
  periodCell.value = `${period} · 총 ${rows.length}건`;
  styleCell(periodCell, { bold: true, size: 10, fill: "FFE8EEF5" });
  for (let c = 2; c <= colCount; c++) {
    styleCell(sheet.getCell(2, c), { bold: true, size: 10, fill: "FFE8EEF5" });
  }
  sheet.getRow(2).height = 22;

  // 3행: 헤더
  const headers = ["일자", "공사명", "공종", "위치", "내용", "사진"];
  const headerRow = sheet.getRow(3);
  headers.forEach((h, i) => {
    const cell = headerRow.getCell(i + 1);
    cell.value = h;
    styleCell(cell, { bold: true, size: 11, fill: "FFD9E2EF" });
  });
  headerRow.height = 24;

  const photoCol = 6;
  const thumbWidth = 150;
  const thumbHeight = 112;
  const dataStart = 4;

  for (let i = 0; i < rows.length; i++) {
    const row = rows[i];
    const excelRow = sheet.getRow(dataStart + i);
    const values = [
      formatDate(row.work_date),
      row.work_name || "",
      row.work_type || "",
      row.location || "",
      row.content || "",
      "",
    ];
    values.forEach((v, idx) => {
      const cell = excelRow.getCell(idx + 1);
      cell.value = v;
      styleCell(cell, { size: 10 });
    });
    excelRow.height = 90;

    try {
      const raw = await getObjectBuffer(row.photo_key);
      if (!raw) {
        excelRow.getCell(photoCol).value = "(사진 없음)";
        continue;
      }
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
