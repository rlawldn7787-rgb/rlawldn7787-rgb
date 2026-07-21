import fs from "fs/promises";
import path from "path";
import { S3Client, PutObjectCommand, GetObjectCommand } from "@aws-sdk/client-s3";
import { v4 as uuidv4 } from "uuid";

const uploadsDir = path.resolve(process.cwd(), "uploads");

function hasR2() {
  return Boolean(
    process.env.R2_ACCOUNT_ID &&
      process.env.R2_ACCESS_KEY_ID &&
      process.env.R2_SECRET_ACCESS_KEY &&
      process.env.R2_BUCKET
  );
}

function getS3() {
  return new S3Client({
    region: "auto",
    endpoint: `https://${process.env.R2_ACCOUNT_ID}.r2.cloudflarestorage.com`,
    credentials: {
      accessKeyId: process.env.R2_ACCESS_KEY_ID!,
      secretAccessKey: process.env.R2_SECRET_ACCESS_KEY!,
    },
  });
}

export async function ensureLocalUploads() {
  if (!hasR2()) {
    await fs.mkdir(uploadsDir, { recursive: true });
  }
}

export async function uploadBuffer(
  buffer: Buffer,
  ext: string,
  contentType: string
): Promise<string> {
  const key = `photos/${uuidv4()}.${ext.replace(".", "")}`;

  if (hasR2()) {
    const s3 = getS3();
    await s3.send(
      new PutObjectCommand({
        Bucket: process.env.R2_BUCKET!,
        Key: key,
        Body: buffer,
        ContentType: contentType,
      })
    );
    return key;
  }

  await fs.mkdir(path.dirname(path.join(uploadsDir, key)), { recursive: true });
  await fs.writeFile(path.join(uploadsDir, key), buffer);
  return key;
}

export function publicUrlForKey(key: string): string {
  if (hasR2() && process.env.R2_PUBLIC_BASE_URL) {
    return `${process.env.R2_PUBLIC_BASE_URL.replace(/\/$/, "")}/${key}`;
  }
  const base = (process.env.PUBLIC_API_URL || "http://localhost:4000").replace(
    /\/$/,
    ""
  );
  return `${base}/uploads/${key}`;
}

export async function getObjectBuffer(key: string): Promise<Buffer | null> {
  if (hasR2()) {
    try {
      const s3 = getS3();
      const result = await s3.send(
        new GetObjectCommand({
          Bucket: process.env.R2_BUCKET!,
          Key: key,
        })
      );
      const bytes = await result.Body?.transformToByteArray();
      return bytes ? Buffer.from(bytes) : null;
    } catch {
      return null;
    }
  }

  try {
    return await fs.readFile(path.join(uploadsDir, key));
  } catch {
    return null;
  }
}

export function getLocalUploadsDir() {
  return uploadsDir;
}

export { hasR2 };
