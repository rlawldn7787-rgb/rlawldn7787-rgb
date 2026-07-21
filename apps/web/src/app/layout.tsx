import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "@/lib/auth";

export const metadata: Metadata = {
  title: "우행통신 보드판",
  description: "현장 사진 보드판 기록 및 엑셀 정리",
  icons: {
    icon: [
      { url: "/favicon.ico" },
      { url: "/icon.png", type: "image/png", sizes: "512x512" },
      { url: "/icon-192.png", type: "image/png", sizes: "192x192" },
    ],
    apple: [{ url: "/apple-touch-icon.png", sizes: "180x180" }],
  },
  openGraph: {
    title: "우행통신 보드판",
    description: "현장 사진 보드판 기록 및 엑셀 정리",
    images: [{ url: "/og.png", width: 1200, height: 630, alt: "우행통신" }],
  },
  twitter: {
    card: "summary_large_image",
    title: "우행통신 보드판",
    description: "현장 사진 보드판 기록 및 엑셀 정리",
    images: ["/og.png"],
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko">
      <body>
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
