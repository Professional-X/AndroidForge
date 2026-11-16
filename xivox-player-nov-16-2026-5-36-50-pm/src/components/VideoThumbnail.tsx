import React, { useState } from 'react';
import { Film, Play } from 'lucide-react';
import { formatTime } from '../data/videos';

interface VideoThumbnailProps {
  src: string;
  title: string;
  duration: number;
  progressRatio?: number;
  aspectClass?: string;
  showPlayOverlay?: boolean;
}

export const VideoThumbnail: React.FC<VideoThumbnailProps> = ({
  src,
  title,
  duration,
  progressRatio,
  aspectClass = 'aspect-video',
  showPlayOverlay = true,
}) => {
  const [failed, setFailed] = useState(false);

  return (
    <div
      className={`relative w-full ${aspectClass} overflow-hidden rounded-xl bg-slate-900 select-none`}
    >
      {src && !failed ? (
        <img
          src={src}
          alt={title}
          referrerPolicy="no-referrer"
          loading="lazy"
          onError={() => setFailed(true)}
          className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-[1.03]"
        />
      ) : (
        <div className="w-full h-full flex flex-col items-center justify-center gap-2 bg-gradient-to-br from-slate-900 via-slate-950 to-indigo-950/70 text-slate-300 p-4">
          <Film className="w-7 h-7 opacity-75" />
          <span className="text-xs font-medium text-center line-clamp-1 opacity-80">{title}</span>
        </div>
      )}

      {/* Measured contrast scrim */}
      <div className="absolute inset-0 bg-gradient-to-t from-black/75 via-black/15 to-transparent pointer-events-none" />

      {showPlayOverlay && (
        <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none">
          <div className="w-11 h-11 rounded-full nebula-accent-gradient flex items-center justify-center text-white shadow-lg nebula-accent-glow">
            <Play className="w-5 h-5 fill-current ml-0.5" />
          </div>
        </div>
      )}

      {/* Tabular duration stamp */}
      <div className="absolute bottom-2 right-2 px-2 py-0.5 rounded-md bg-black/75 backdrop-blur-md text-[11px] font-mono-tabular font-medium text-white tracking-tight">
        {formatTime(duration)}
      </div>

      {/* Continue watching progress bar */}
      {typeof progressRatio === 'number' && progressRatio > 0.01 && (
        <div className="absolute bottom-0 left-0 right-0 h-1 bg-white/20">
          <div
            className="h-full nebula-accent-gradient transition-all duration-300"
            style={{ width: `${Math.min(100, Math.max(3, progressRatio * 100))}%` }}
          />
        </div>
      )}
    </div>
  );
};
