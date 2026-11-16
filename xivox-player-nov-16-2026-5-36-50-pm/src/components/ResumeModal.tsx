import React from 'react';
import { Play, RotateCcw, X } from 'lucide-react';
import { VideoItem } from '../types/nebula';
import { formatTime } from '../data/videos';

interface ResumeModalProps {
  video: VideoItem;
  savedPosition: number;
  onResume: () => void;
  onStartBeginning: () => void;
  onClose: () => void;
}

export const ResumeModal: React.FC<ResumeModalProps> = ({
  video,
  savedPosition,
  onResume,
  onStartBeginning,
  onClose,
}) => {
  const formattedTime = formatTime(savedPosition);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="resume-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md"
    >
      <div className="w-full max-w-md nebula-card rounded-3xl p-6 space-y-5 shadow-2xl border border-[rgba(var(--nebula-accent-rgb),0.4)]">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h2
              id="resume-modal-title"
              className="font-display font-bold text-lg text-[var(--nebula-accent)] tracking-wide"
            >
              RESUME FROM {formattedTime}?
            </h2>
            <p className="text-sm text-[var(--nebula-text)] font-medium mt-1 line-clamp-1">
              {video.title}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close resume dialog"
            className="min-h-[40px] min-w-[40px] rounded-xl flex items-center justify-center text-[var(--nebula-text-muted)] hover:text-[var(--nebula-text)]"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="flex flex-col gap-2.5">
          <button
            type="button"
            onClick={onResume}
            className="w-full min-h-[48px] px-5 py-3 rounded-xl nebula-accent-gradient nebula-accent-glow text-white font-semibold text-sm flex items-center justify-center gap-2 active:scale-[0.98] transition-transform"
          >
            <Play className="w-4 h-4 fill-current" />
            <span>Resume from {formattedTime}</span>
          </button>

          <button
            type="button"
            onClick={onStartBeginning}
            className="w-full min-h-[48px] px-5 py-3 rounded-xl border border-[var(--nebula-border)] text-[var(--nebula-text)] font-medium text-sm flex items-center justify-center gap-2 hover:border-[var(--nebula-accent)] transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
            <span>Play from beginning</span>
          </button>
        </div>
      </div>
    </div>
  );
};
