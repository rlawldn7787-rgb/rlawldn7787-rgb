import { Router } from "express";
import {
  findUserByUsername,
  signToken,
  verifyPassword,
  requireAuth,
} from "../auth";

export const authRouter = Router();

authRouter.post("/login", async (req, res) => {
  const { username, password } = req.body as {
    username?: string;
    password?: string;
  };

  if (!username || !password) {
    return res.status(400).json({ error: "아이디와 비밀번호를 입력하세요." });
  }

  const user = await findUserByUsername(username);
  if (!user || !user.active) {
    return res.status(401).json({ error: "아이디 또는 비밀번호가 올바르지 않습니다." });
  }

  const ok = await verifyPassword(password, user.password_hash);
  if (!ok) {
    return res.status(401).json({ error: "아이디 또는 비밀번호가 올바르지 않습니다." });
  }

  const authUser = {
    id: user.id,
    username: user.username,
    name: user.name,
    role: user.role,
  };

  return res.json({
    token: signToken(authUser),
    user: authUser,
  });
});

authRouter.get("/me", requireAuth, (req, res) => {
  return res.json({ user: req.user });
});
