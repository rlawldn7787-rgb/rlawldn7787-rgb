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

const leftAlign: Partial<ExcelJS.Alignment> = {
  horizontal: "left",
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
    align?: Partial<ExcelJS.Alignment>;
  } = {}
) {
  cell.alignment = { ...(opts.align ?? centerAlign) };
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

function photoRef(index: number) {
  return `사진-${index}`;
}

function photoCaption(index: number, row: ExportRow) {
  const date = formatDate(row.work_date);
  const workName = row.work_name || "-";
  const workType = row.work_type || "-";
  const location = row.location || "-";
  const content = row.content || "-";
  return [
    `[${photoRef(index)}] 현장 보드판 사진`,
    `일자: ${date}`,
    `공사명: ${workName}`,
    `공종: ${workType}`,
    `위치: ${location}`,
    `내용: ${content}`,
  ].join("\n");
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
  const photoSheet = workbook.addWorksheet("사진");

  const colCount = 6;
  sheet.columns = [
    { key: "work_date", width: 12 },
    { key: "work_name", width: 22 },
    { key: "work_type", width: 14 },
    { key: "location", width: 18 },
    { key: "content", width: 32 },
    { key: "photo", width: 12 },
  ];

  // 1행: 제목
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

  // 2행: 기간 + 안내
  sheet.mergeCells(2, 1, 2, colCount);
  const period =
    options.from || options.to
      ? `조회기간: ${options.from || "전체"} ~ ${options.to || "전체"}`
      : `조회기간: 전체`;
  const periodCell = sheet.getCell(2, 1);
  periodCell.value = `${period} · 총 ${rows.length}건 · 사진은 '사진' 시트 참고`;
  styleCell(periodCell, { bold: true, size: 10, fill: "FFE8EEF5" });
  for (let c = 2; c <= colCount; c++) {
    styleCell(sheet.getCell(2, c), { bold: true, size: 10, fill: "FFE8EEF5" });
  }
  sheet.getRow(2).height = 22;

  // 3행: 헤더
  const headers = ["일자", "공사명", "공종", "위치", "내용", "사진번호"];
  const headerRow = sheet.getRow(3);
  headers.forEach((h, i) => {
    const cell = headerRow.getCell(i + 1);
    cell.value = h;
    styleCell(cell, { bold: true, size: 11, fill: "FFD9E2EF" });
  });
  headerRow.height = 24;

  const dataStart = 4;
  for (let i = 0; i < rows.length; i++) {
    const row = rows[i];
    const excelRow = sheet.getRow(dataStart + i);
    const ref = photoRef(i + 1);
    const values = [
      formatDate(row.work_date),
      row.work_name || "",
      row.work_type || "",
      row.location || "",
      row.content || "",
      ref,
    ];
    values.forEach((v, idx) => {
      const cell = excelRow.getCell(idx + 1);
      cell.value = v;
      styleCell(cell, { size: 10 });
    });
    excelRow.height = 22;
  }

  // ---- 사진 시트 ----
  photoSheet.columns = [
    { key: "info", width: 36 },
    { key: "photo", width: 48 },
  ];

  photoSheet.mergeCells(1, 1, 1, 2);
  const photoTitle = photoSheet.getCell(1, 1);
  photoTitle.value = "현장 보드판 사진 모음";
  styleCell(photoTitle, {
    bold: true,
    size: 14,
    fill: "FF0B1F3A",
    color: "FFFFFFFF",
  });
  styleCell(photoSheet.getCell(1, 2), {
    bold: true,
    size: 14,
    fill: "FF0B1F3A",
    color: "FFFFFFFF",
  });
  photoSheet.getRow(1).height = 28;

  photoSheet.mergeCells(2, 1, 2, 2);
  const guide = photoSheet.getCell(2, 1);
  guide.value =
    "이 시트는 현장기록 표의 '사진번호'에 대응하는 원본 보드판 사진입니다. 왼쪽 설명과 오른쪽 사진을 함께 확인하세요.";
  styleCell(guide, {
    bold: false,
    size: 10,
    fill: "FFE8EEF5",
    align: leftAlign,
  });
  styleCell(photoSheet.getCell(2, 2), {
    size: 10,
    fill: "FFE8EEF5",
    align: leftAlign,
  });
  photoSheet.getRow(2).height = 36;

  const photoW = 320;
  const photoH = 240;
  let cursorRow = 4;

  for (let i = 0; i < rows.length; i++) {
    const row = rows[i];
    const ref = photoRef(i + 1);
    const infoRow = cursorRow;
    const imageRow = cursorRow + 1;

    // 설명 헤더
    photoSheet.mergeCells(infoRow, 1, infoRow, 2);
    const infoCell = photoSheet.getCell(infoRow, 1);
    infoCell.value = photoCaption(i + 1, row);
    styleCell(infoCell, {
      bold: true,
      size: 10,
      fill: "FFD9E2EF",
      align: leftAlign,
    });
    styleCell(photoSheet.getCell(infoRow, 2), {
      size: 10,
      fill: "FFD9E2EF",
      align: leftAlign,
    });
    photoSheet.getRow(infoRow).height = 78;

    // 사진 영역
    const labelCell = photoSheet.getCell(imageRow, 1);
    labelCell.value = `${ref}\n(우측 사진)`;
    styleCell(labelCell, { size: 10, align: centerAlign });

    const photoCell = photoSheet.getCell(imageRow, 2);
    photoCell.value = "";
    styleCell(photoCell, { size: 10 });
    photoSheet.getRow(imageRow).height = 190;

    try {
      const raw = await getObjectBuffer(row.photo_key);
      if (!raw) {
        photoCell.value = "(사진 없음)";
      } else {
        const thumb = await sharp(raw)
          .rotate()
          .resize({
            width: photoW * 2,
            height: photoH * 2,
            fit: "inside",
            withoutEnlargement: true,
          })
          .jpeg({ quality: 85 })
          .toBuffer();

        const imageId = workbook.addImage({
          buffer: Uint8Array.from(thumb) as unknown as ExcelJS.Buffer,
          extension: "jpeg",
        });

        photoSheet.addImage(imageId, {
          tl: { col: 1, row: imageRow - 1 },
          ext: { width: photoW, height: photoH },
          editAs: "oneCell",
        });
      }
    } catch (err) {
      console.warn("Failed to embed photo sheet image", ref, err);
      photoCell.value = "(사진 불러오기 실패)";
    }

    cursorRow += 3; // 한 칸 여백
  }

  if (rows.length === 0) {
    photoSheet.getCell(4, 1).value = "해당 기간에 등록된 사진이 없습니다.";
    styleCell(photoSheet.getCell(4, 1), { size: 11, align: leftAlign });
  }

  const arrayBuffer = await workbook.xlsx.writeBuffer();
  return Buffer.from(arrayBuffer);
}
