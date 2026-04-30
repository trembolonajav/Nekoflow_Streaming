import { useState } from "react";
import { Heart, MessageCircle, Share2 } from "lucide-react";
import { cn } from "@/lib/utils";
import type { CommentDto } from "@/lib/backend-api";
import { CommentComposer } from "./CommentComposer";

interface CommentItemProps {
  comment: CommentDto;
  depth?: 0 | 1;
  onReply?: (parentId: string, body: string) => void | Promise<void>;
}

export function CommentItem({ comment, depth = 0, onReply }: CommentItemProps) {
  const [liked, setLiked] = useState(false);
  const [likes, setLikes] = useState(0);
  const [replyOpen, setReplyOpen] = useState(false);
  const [showAllReplies, setShowAllReplies] = useState(false);

  const isReply = depth === 1;
  const replies = comment.replies ?? [];
  const visibleReplies = showAllReplies ? replies : replies.slice(0, 2);
  const hiddenCount = replies.length - visibleReplies.length;

  const toggleLike = () => {
    setLiked((prev) => {
      setLikes((current) => (prev ? current - 1 : current + 1));
      return !prev;
    });
  };

  return (
    <article
      className={cn(
        "relative flex gap-3",
        isReply && "before:absolute before:bottom-2 before:left-[15px] before:top-0 before:w-px before:bg-border-subtle",
      )}
    >
      <div className="relative flex-shrink-0">
        {comment.user.avatarUrl ? (
          <img
            src={comment.user.avatarUrl}
            alt=""
            className={cn(
              "rounded-full object-cover",
              isReply ? "h-8 w-8" : "h-10 w-10",
              comment.user.badge === "curador" && "ring-2 ring-gold/70 ring-offset-2 ring-offset-background",
              comment.user.badge === "fundador" && "ring-2 ring-ivory/40 ring-offset-2 ring-offset-background",
            )}
          />
        ) : (
          <span
            className={cn(
              "flex items-center justify-center rounded-full border border-gold/20 bg-onyx font-serif text-gold",
              isReply ? "h-8 w-8 text-sm" : "h-10 w-10",
            )}
          >
            {comment.user.name.charAt(0).toUpperCase()}
          </span>
        )}
      </div>

      <div className="flex min-w-0 flex-1 flex-col gap-1.5">
        <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5">
          <span className="font-serif text-sm font-medium text-ivory">{comment.user.name}</span>
          <span className="font-mono text-[11px] text-ivory-muted">@{comment.user.handle}</span>
          {comment.user.badge ? (
            <span
              className={cn(
                "rounded-full border px-1.5 py-0.5 font-mono text-[9px] uppercase tracking-[0.2em]",
                comment.user.badge === "curador"
                  ? "border-gold/50 text-gold"
                  : "border-ivory/30 text-ivory-muted",
              )}
            >
              {comment.user.badge}
            </span>
          ) : null}
          {comment.createdAt ? (
            <span className="font-mono text-[11px] text-ivory-muted/70">· {new Date(comment.createdAt).toLocaleString("pt-BR")}</span>
          ) : null}
        </div>

        <p className={cn("leading-relaxed text-ivory/90", isReply ? "text-[13px]" : "text-sm")}>
          {renderBody(comment.body)}
        </p>

        <div className="mt-1 flex items-center gap-1">
          <ActionButton
            onClick={toggleLike}
            active={liked}
            label={String(likes)}
            icon={<Heart className={cn("h-3.5 w-3.5", liked && "fill-gold")} />}
          />
          {!isReply && onReply ? (
            <ActionButton
              onClick={() => setReplyOpen((value) => !value)}
              active={replyOpen}
              label="Responder"
              icon={<MessageCircle className="h-3.5 w-3.5" />}
            />
          ) : null}
          <ActionButton label="Compartilhar" icon={<Share2 className="h-3.5 w-3.5" />} />
        </div>

        {!isReply && replyOpen && onReply ? (
          <CommentComposer
            variant="inline"
            autoFocus
            initialValue={`@${comment.user.handle} `}
            onSubmit={(body) => onReply(comment.id, body)}
            onCancel={() => setReplyOpen(false)}
          />
        ) : null}

        {!isReply && replies.length > 0 ? (
          <div className="mt-3 flex flex-col gap-4 border-l border-border-subtle pl-5">
            {visibleReplies.map((reply) => (
              <CommentItem key={reply.id} comment={reply} depth={1} />
            ))}
            {hiddenCount > 0 ? (
              <button
                type="button"
                onClick={() => setShowAllReplies(true)}
                className="self-start font-mono text-[10px] uppercase tracking-[0.22em] text-gold transition-opacity hover:opacity-80"
              >
                + Ver mais {hiddenCount} {hiddenCount === 1 ? "resposta" : "respostas"}
              </button>
            ) : null}
          </div>
        ) : null}
      </div>
    </article>
  );
}

function ActionButton({
  icon,
  label,
  onClick,
  active,
}: {
  icon: React.ReactNode;
  label: string;
  onClick?: () => void;
  active?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] uppercase tracking-[0.18em] transition-colors",
        active
          ? "text-gold"
          : "text-ivory-muted hover:bg-surface-elevated/60 hover:text-gold",
      )}
    >
      {icon}
      <span className="font-mono">{label}</span>
    </button>
  );
}

function renderBody(body: string) {
  const parts = body.split(/(@[a-zA-Z0-9._-]+)/g);
  return parts.map((part, index) => {
    if (part.startsWith("@")) {
      return (
        <span key={index} className="font-mono text-gold">
          {part}
        </span>
      );
    }
    return <span key={index}>{part}</span>;
  });
}
