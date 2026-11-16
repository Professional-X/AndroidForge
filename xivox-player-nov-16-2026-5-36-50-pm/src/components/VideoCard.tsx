import React from 'react';
import { Heart, Info, Play } from 'lucide-react';
import { motion } from 'motion/react';
import { AnimationLevel, LayoutMode, PlaybackProgress, VideoItem } from '../types/nebula';
import { formatTime } from '../data/videos';
import { VideoThumbnail } from './VideoThumbnail';

interface VideoCardProps {
  video: VideoItem;
  layout: LayoutMode;
  isFavorite: boolean;
  progress?: PlaybackProgress;
  animationLevel: AnimationLevel;
  onPlay: (video: VideoItem) => void;
  onOpenDetails?: (video: VideoItem) => void;
  onToggleFavorite: (videoId: string) => void;
}

export const VideoCard: React.FC<VideoCardProps> = ({
  video,
  layout,
  isFavorite,
  progress,
  animationLevel,
  onPlay,
  onOpenDetails,
  onToggleFavorite,
}) => {
  const ratio =
    progress && progress.duration > 0 ? progress.position / progress.duration : undefined;
  const remaining =
    progress && progress.duration > progress.position
      ? Math.max(0, progress.duration - progress.position)
      : undefined;

  const hoverMotion =
    animationLevel === 'full' ? { y: -2 } : animationLevel === 'reduced' ? { opacity: 0.96 } : {};

  if (layout === 'list') {
    return (
      <motion.article
        whileHover={hoverMotion}
        transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
        className="group nebula-card nebula-border-glow rounded-2xl p-3 flex items-center gap-3.5 transition-colors"
      >
        <button
          type="button"
          onClick={() => onPlay(video)}
          className="w-32 sm:w-44 shrink-0 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--nebula-accent)] rounded-xl"
          aria-label={`Play ${video.title}`}
        >
          <VideoThumbnail
            src={video.thumbnail}
            title={video.title}
            duration={video.duration}
            progressRatio={ratio}
          />
        </button>

        <button
          type="button"
          onClick={() => (onOpenDetails ? onOpenDetails(video) : onPlay(video))}
          className="flex-1 min-w-0 text-left py-1 focus-visible:outline-none"
        >
          <h3 className="font-display font-semibold text-base text-[var(--nebula-text)] truncate">
            {video.title}
          </h3>
          <p className="text-xs text-[var(--nebula-text-muted)] line-clamp-1 mt-0.5">
            {video.subtitle}
          </p>
          <div className="flex items-center gap-1.5 text-xs text-[var(--nebula-text-muted)] mt-2 truncate">
            <span>{video.category}</span>
            <span aria-hidden="true">·</span>
            <span>{video.resolution}</span>
            <span aria-hidden="true">·</span>
            <span className="font-mono-tabular">{video.sizeLabel}</span>
            {remaining !== undefined && remaining > 3 && (
              <>
                <span aria-hidden="true">·</span>
                <span className="font-mono-tabular text-[var(--nebula-accent)] font-medium">
                  {formatTime(remaining)} left
                </span>
              </>
            )}
          </div>
        </button>

        <div className="flex items-center gap-1 shrink-0">
          {onOpenDetails && (
            <button
              type="button"
              onClick={() => onOpenDetails(video)}
              aria-label={`Details for ${video.title}`}
              className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl text-[var(--nebula-text-muted)] hover:text-[var(--nebula-text)] transition-colors"
            >
              <Info className="w-4 h-4" />
            </button>
          )}
          <button
            type="button"
            onClick={() => onToggleFavorite(video.id)}
            aria-label={isFavorite ? `Remove ${video.title} from favorites` : `Add ${video.title} to favorites`}
            className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl text-[var(--nebula-text-muted)] hover:text-[var(--nebula-accent)] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--nebula-accent)]"
          >
            <Heart
              className={`w-5 h-5 transition-transform duration-150 ${
                isFavorite ? 'fill-[var(--nebula-accent)] text-[var(--nebula-accent)] scale-110' : ''
              }`}
            />
          </button>
          <button
            type="button"
            onClick={() => onPlay(video)}
            aria-label={`Play ${video.title}`}
            className="hidden sm:flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl nebula-accent-gradient text-white shadow-sm active:scale-95 transition-transform"
          >
            <Play className="w-4 h-4 fill-current ml-0.5" />
          </button>
        </div>
      </motion.article>
    );
  }

  return (
    <motion.article
      whileHover={hoverMotion}
      transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
      className="group nebula-card nebula-border-glow rounded-2xl p-3 flex flex-col gap-3 transition-colors"
    >
      <button
        type="button"
        onClick={() => onPlay(video)}
        className="w-full text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--nebula-accent)] rounded-xl"
        aria-label={`Play ${video.title}`}
      >
        <VideoThumbnail
          src={video.thumbnail}
          title={video.title}
          duration={video.duration}
          progressRatio={ratio}
        />
      </button>

      <div className="flex items-start justify-between gap-2 px-0.5">
        <button
          type="button"
          onClick={() => (onOpenDetails ? onOpenDetails(video) : onPlay(video))}
          className="flex-1 min-w-0 text-left focus-visible:outline-none"
        >
          <h3 className="font-display font-semibold text-base text-[var(--nebula-text)] truncate">
            {video.title}
          </h3>
          <div className="flex items-center gap-1.5 text-xs text-[var(--nebula-text-muted)] mt-1 truncate">
            <span>{video.category}</span>
            <span aria-hidden="true">·</span>
            <span>{video.resolution}</span>
            <span aria-hidden="true">·</span>
            <span className="font-mono-tabular">{video.sizeLabel}</span>
          </div>
          {remaining !== undefined && remaining > 3 && (
            <p className="text-xs font-mono-tabular text-[var(--nebula-accent)] font-medium mt-1">
              {formatTime(remaining)} remaining
            </p>
          )}
        </button>

        <div className="flex items-center -mr-1.5 -mt-1 shrink-0">
          {onOpenDetails && (
            <button
              type="button"
              onClick={() => onOpenDetails(video)}
              aria-label={`Details for ${video.title}`}
              className="min-h-[44px] min-w-[40px] flex items-center justify-center rounded-xl text-[var(--nebula-text-muted)] hover:text-[var(--nebula-text)] transition-colors"
            >
              <Info className="w-4 h-4" />
            </button>
          )}
          <button
            type="button"
            onClick={() => onToggleFavorite(video.id)}
            aria-label={isFavorite ? `Remove ${video.title} from favorites` : `Add ${video.title} to favorites`}
            className="min-h-[44px] min-w-[40px] flex items-center justify-center rounded-xl text-[var(--nebula-text-muted)] hover:text-[var(--nebula-accent)] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--nebula-accent)]"
          >
            <Heart
              className={`w-5 h-5 transition-transform duration-150 ${
                isFavorite ? 'fill-[var(--nebula-accent)] text-[var(--nebula-accent)] scale-110' : ''
              }`}
            />
          </button>
        </div>
      </div>
    </motion.article>
  );
};
