// Supabase Edge Function: delete-account
// Deploy via Supabase Dashboard -> Edge Functions -> Deploy a new function -> Via Editor
// Function name: delete-account

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function jsonError(status: number, code: string, message: string) {
  return new Response(JSON.stringify({ code, message }), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (req.method !== "POST" && req.method !== "DELETE") {
    return jsonError(405, "METHOD_NOT_ALLOWED", "Metode tidak diizinkan.");
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const authHeader = req.headers.get("Authorization") ?? "";

    if (!authHeader.startsWith("Bearer ")) {
      return jsonError(401, "UNAUTHORIZED", "Token otorisasi diperlukan.");
    }

    const body = await req.json().catch(() => ({}));
    const password = typeof body?.password === "string" ? body.password : "";
    if (!password) {
      return jsonError(400, "INVALID_PASSWORD", "Kata sandi konfirmasi wajib diisi.");
    }

    // 1. Verifikasi sesi pengguna saat ini dari JWT
    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
    });
    const {
      data: { user },
      error: userErr,
    } = await userClient.auth.getUser();

    if (userErr || !user || !user.email) {
      return jsonError(401, "TOKEN_EXPIRED", "Sesi tidak valid atau kedaluwarsa.");
    }

    // 2. Verifikasi kata sandi konfirmasi via Supabase Auth
    const verifyClient = createClient(supabaseUrl, anonKey);
    const { error: signInErr } = await verifyClient.auth.signInWithPassword({
      email: user.email,
      password,
    });
    if (signInErr) {
      return jsonError(401, "INVALID_PASSWORD", "Kata sandi konfirmasi salah.");
    }

    // 3. Hapus seluruh file lampiran di bucket attachments/{userId}/
    const adminClient = createClient(supabaseUrl, serviceRoleKey);
    let hasMore = true;
    while (hasMore) {
      const { data: files, error: listErr } = await adminClient.storage
        .from("attachments")
        .list(user.id, { limit: 100 });
      if (listErr || !files || files.length === 0) {
        hasMore = false;
      } else {
        const paths = files.map((f) => `${user.id}/${f.name}`);
        await adminClient.storage.from("attachments").remove(paths);
        if (files.length < 100) hasMore = false;
      }
    }

    // 4. Hapus akun dari auth.users (ON DELETE CASCADE menghapus semua tabel user)
    const { error: deleteErr } = await adminClient.auth.admin.deleteUser(user.id);
    if (deleteErr) {
      return jsonError(500, "DELETE_FAILED", "Gagal menghapus akun dari server.");
    }

    return new Response(JSON.stringify({ deleted: true }), {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    return jsonError(500, "INTERNAL_ERROR", err instanceof Error ? err.message : "Terjadi kesalahan server.");
  }
});
