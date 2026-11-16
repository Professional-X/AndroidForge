import React from 'react';
import { Captions, Heart, Play, RotateCcw, X } from 'lucide-react';
import { PlaybackProgress, VideoItem } from '../types/nebula';
import { formatTime } from '../data/videos';
import { VideoThumbnail } from './VideoThumbnail';

interface VideoDetailsModalProps {
  video: VideoItem;
  progress?: PlaybackProgress;
  isFavorite: boolean;
  onPlay: (video: VideoItem, startFromZero?: boolean) => void;
  onToggleFavorite: (videoId: string) => void;
  onClose: () => void;
}

export const VideoDetailsModal: React.FC<VideoDetailsModalProps> = ({
  video,
  progress,
  isFavorite,
  onPlay,
  onToggleFavorite,
  onClose,
}) => {
  const hasResume = Boolean(progress && progress.position >= 5 && progress.position < progress.duration - 5);
  const ratio = progress && progress.duration > 0 ? progress.position / progress.duration : undefined;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="video-details-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md"
    >
      <div className="w-full max-w-xl nebula-card rounded-3xl p-5 sm:p-6 space-y-5 shadow-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between gap-3">
          <h2 id="video-details-title" className="font-display font-bold text-lg text-[var(--nebula-text)]">
            Video Details
          </h2>
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => onToggleFavorite(video.id)}
              aria-label={isFavorite ? 'Remove favorite' : 'Add favorite'}
              className="min-h-[40px] min-w-[40px] rounded-xl flex items-center justify-center text-[var(--nebula-text-muted)] hover:text-[var(--nebula-accent)]"
            >
              <Heart className={`w-5 h-5 ${isFavorite ? 'fill-[var(--nebula-accent)] text-[var(--nebula-accent)]' : ''}`} />
            </button>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close details"
              className="min-h-[40px] min-w-[40px] rounded-xl flex items-center justify-center text-[var(--nebula-text-muted)] hover:text-[var(--nebula-text)]"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        <VideoThumbnail
          src={video.thumbnail}
          title={video.title}
          duration={video.duration}
          progressRatio={ratio}
        />

        <div>
          <h3 className="font-display font-bold text-xl text-[var(--nebula-text)]">{video.title}</h3>
          <p className="text-sm text-[var(--nebula-text-muted)] mt-1">{video.subtitle}</p>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 py-3 border-y border-[var(--nebula-border)] text-xs">
          <div>
            <p className="text-[var(--nebula-text-muted)]">Resolution</p>
            <p className="font-semibold text-[var(--nebula-text)] mt-0.5">{video.resolution}</p>
          </div>
          <div>
            <p className="text-[var(--nebula-text-muted)]">Duration</p>
            <p className="font-mono-tabular font-semibold text-[var(--nebula-text)] mt-0.5">{formatTime(video.duration)}</p>
          </div>
          <div>
            <p className="text-[var(--nebula-text-muted)]">File Size</p>
            <p className="font-mono-tabular font-semibold text-[var(--nebula-text)] mt-0.5">{video.sizeLabel}</p>
          </div>
          <div>
            <p className="text-[var(--nebula-text-muted)]">Subtitles</p>
            <p className="font-semibold text-[var(--nebula-text)] mt-0.5 flex items-center gap-1">
              <Captions className="w-3.5 h-3.5 text-[var(--nebula-accent)]" />
              <span>{video.subtitles.length > 0 ? `${video.subtitles.length} cues` : 'Available'}</span>
            </p>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row gap-2.5">
          <button
            type="button"
            onClick={() => onPlay(video, false)}
            className="flex-1 min-h-[48px] px-5 py-3 rounded-xl nebula-accent-gradient nebula-accent-glow text-white font-semibold text-sm flex items-center justify-center gap-2"
          >
            <Play className="w-4 h-4 fill-current" />
            <span>{hasResume ? `Resume from ${formatTime(progress!.position)}` : 'Play Video'}</span>
          </button>

          {hasResume && (
            <button
              type="button"
              onClick={() => onPlay(video, true)}
              className="min-h-[48px] px-4 py-3 rounded-xl border border-[var(--nebula-border)] text-[var(--nebula-text)] font-medium text-sm flex items-center justify-center gap-2 hover:border-[var(--nebula-accent)]"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Beginning</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
