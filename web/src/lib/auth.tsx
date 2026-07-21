"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { fetchMe, User } from "@/lib/api";

type AuthState = {
  token: string | null;
  user: User | null;
  loading: boolean;
  setSession: (token: string, user: User) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthState | null>(null);
const TOKEN_KEY = "woohaeng_token";

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const saved = localStorage.getItem(TOKEN_KEY);
    if (!saved) {
      setLoading(false);
      return;
    }
    fetchMe(saved)
      .then((data) => {
        setToken(saved);
        setUser(data.user);
      })
      .catch(() => {
        localStorage.removeItem(TOKEN_KEY);
      })
      .finally(() => setLoading(false));
  }, []);

  const setSession = useCallback((nextToken: string, nextUser: User) => {
    localStorage.setItem(TOKEN_KEY, nextToken);
    setToken(nextToken);
    setUser(nextUser);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    setToken(null);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ token, user, loading, setSession, logout }),
    [token, user, loading, setSession, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
