/** 앱·웹·API에서 공유하는 보드판 필드 정의 */
export const BOARD_FIELDS = [
  { key: "workName", label: "공사명", required: true },
  { key: "workType", label: "공종", required: false },
  { key: "location", label: "위치", required: false },
  { key: "content", label: "내용", required: false },
  { key: "workDate", label: "일자", required: true },
] as const;

export const EXCEL_COLUMNS = [
  "일자",
  "공사명",
  "공종",
  "위치",
  "내용",
  "작성자",
  "업로드시각",
  "사진",
] as const;
