import { describe, it, expect } from "vitest";
import { render } from "@testing-library/react";

import { CommentItem } from "./CommentItem";
import type { CommentDto } from "@/lib/backend-api";

function comment(body: string): CommentDto {
  return {
    id: "c1",
    animeId: "a1",
    episodeId: "e1",
    body,
    containsSpoiler: false,
    createdAt: new Date().toISOString(),
    user: { id: "u1", name: "Alice", handle: "alice", avatarUrl: null, badge: null },
    replies: [],
  };
}

describe("CommentItem — tratamento de conteudo do usuario", () => {
  it("renderiza <script> no comentario como texto, nao como elemento", () => {
    const payload = "<script>alert('xss')</script>";
    const { container } = render(<CommentItem comment={comment(payload)} />);

    // React escapa por padrao: nenhum elemento <script> e criado.
    expect(container.querySelector("script")).toBeNull();
    // O conteudo aparece como texto literal.
    expect(container.textContent).toContain(payload);
  });

  it("nao cria <img onerror> a partir de texto do usuario", () => {
    const payload = "<img src=x onerror=alert(1)>";
    const { container } = render(<CommentItem comment={comment(payload)} />);

    expect(container.querySelector("img[onerror]")).toBeNull();
    expect(container.textContent).toContain(payload);
  });

  it("escapa script mesmo junto de uma mencao @user", () => {
    const payload = "@alice <script>steal()</script>";
    const { container } = render(<CommentItem comment={comment(payload)} />);

    expect(container.querySelector("script")).toBeNull();
    expect(container.textContent).toContain("<script>steal()</script>");
  });
});
