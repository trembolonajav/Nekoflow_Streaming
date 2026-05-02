import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { MessageSquare } from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/lib/utils";
import {
  createCommentReply,
  createEpisodeComment,
  fetchEpisodeComments,
  type CommentDto,
} from "@/lib/backend-api";
import { CommentComposer } from "./CommentComposer";
import { CommentItem } from "./CommentItem";

interface EpisodeCommentsProps {
  episodeId: string;
  episodeTitle: string;
}

type SortMode = "relevance" | "recent";

export function EpisodeComments({ episodeId, episodeTitle }: EpisodeCommentsProps) {
  const queryClient = useQueryClient();
  const [sort, setSort] = useState<SortMode>("relevance");
  const commentsQuery = useQuery({
    queryKey: ["episode-comments", episodeId],
    queryFn: () => fetchEpisodeComments(episodeId),
  });

  const createCommentMutation = useMutation({
    mutationFn: (body: string) => createEpisodeComment(episodeId, { body }),
    onSuccess: () => {
      toast.success("Comentário enviado.");
      void queryClient.invalidateQueries({ queryKey: ["episode-comments", episodeId] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const createReplyMutation = useMutation({
    mutationFn: ({ commentId, body }: { commentId: string; body: string }) =>
      createCommentReply(commentId, { body }),
    onSuccess: () => {
      toast.success("Resposta enviada.");
      void queryClient.invalidateQueries({ queryKey: ["episode-comments", episodeId] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const thread = useMemo(() => commentsQuery.data ?? [], [commentsQuery.data]);
  const sorted = useMemo(() => {
    const byReplyCount = (comment: CommentDto) => comment.replies.length;
    if (sort === "relevance") {
      return [...thread].sort((a, b) => byReplyCount(b) - byReplyCount(a));
    }
    return [...thread].sort((a, b) => (b.createdAt ?? "").localeCompare(a.createdAt ?? ""));
  }, [thread, sort]);

  const totalCount = useMemo(
    () => thread.reduce((acc, comment) => acc + 1 + (comment.replies?.length ?? 0), 0),
    [thread],
  );

  return (
    <section
      aria-label={`Comentários sobre ${episodeTitle}`}
      className="mx-auto w-full max-w-[1500px] px-6 py-12 md:px-10 md:py-16"
    >
      <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <span className="inline-flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.32em] text-gold">
            <span className="h-px w-8 bg-gold/60" />
            <MessageSquare className="h-3 w-3" />
            Conversa
          </span>
          <h2 className="mt-3 font-serif text-3xl font-medium leading-tight text-ivory md:text-4xl">
            Sobre este episódio
          </h2>
          <p className="mt-2 font-mono text-[11px] uppercase tracking-[0.22em] text-ivory-muted">
            {totalCount} {totalCount === 1 ? "comentário" : "comentários"}
          </p>
        </div>

        <div
          role="tablist"
          aria-label="Ordenar comentários"
          className="inline-flex rounded-full border border-border-subtle bg-surface/60 p-1 backdrop-blur"
        >
          {([
            { id: "relevance", label: "Mais relevantes" },
            { id: "recent", label: "Mais recentes" },
          ] as Array<{ id: SortMode; label: string }>).map((option) => (
            <button
              key={option.id}
              role="tab"
              aria-selected={sort === option.id}
              onClick={() => setSort(option.id)}
              className={cn(
                "rounded-full px-3.5 py-1.5 text-[10px] uppercase tracking-[0.22em] transition-all duration-200",
                sort === option.id ? "bg-gold/10 text-gold" : "text-ivory-muted hover:text-gold",
              )}
            >
              {option.label}
            </button>
          ))}
        </div>
      </header>

      <CommentComposer onSubmit={(body) => createCommentMutation.mutateAsync(body)} />

      {commentsQuery.isLoading ? (
        <div className="mt-8 rounded-xl border border-border-subtle bg-surface/40 p-8 text-center text-sm text-ivory-muted">
          Carregando comentários...
        </div>
      ) : null}

      {!commentsQuery.isLoading ? (
        <div className="mt-8 flex flex-col divide-y divide-border-subtle">
          {sorted.map((comment) => (
            <div key={comment.id} className="py-6 first:pt-0">
              <CommentItem
                comment={comment}
                onReply={(commentId, body) => createReplyMutation.mutateAsync({ commentId, body })}
              />
            </div>
          ))}
        </div>
      ) : null}

      {!commentsQuery.isLoading && sorted.length === 0 ? (
        <div className="mt-8 rounded-xl border border-dashed border-border-subtle bg-surface/30 py-16 text-center text-sm text-ivory-muted">
          Ainda não há comentários neste episódio.
        </div>
      ) : null}

      <p className="mt-10 text-center font-mono text-[10px] uppercase tracking-[0.3em] text-ivory-muted/60">
        Conversa moderada pela curadoria NekoFlow
      </p>
    </section>
  );
}
