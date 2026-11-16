import React from 'react';
import { Heart, Film } from 'lucide-react';
import { AnimationLevel, LayoutMode, NavTab, PlaybackProgress, VideoItem } from '../types/nebula';
import { VideoCard } from './VideoCard';

interface FavoritesViewProps {
  videos: VideoItem[];
  favorites: string[];
  layoutMode: LayoutMode;
  progressMap: Record<string, PlaybackProgress>;
  animationLevel: AnimationLevel;
  onPlay: (video: VideoItem) => void;
  onOpenDetails: (video: VideoItem) => void;
  onToggleFavorite: (videoId: string) => void;
  onNavigate: (tab: NavTab) => void;
}

export const FavoritesView: React.FC<FavoritesViewProps> = ({
  videos,
  favorites,
  layoutMode,
  progressMap,
  animationLevel,
  onPlay,
  onOpenDetails,
  onToggleFavorite,
  onNavigate,
}) => {
  const favoriteVideos = videos.filter((v) => favorites.includes(v.id));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display font-bold text-2xl sm:text-3xl tracking-tight text-[var(--nebula-text)]">
          Favorites
        </h1>
        <p className="text-sm text-[var(--nebula-text-muted)] mt-0.5">
          {favoriteVideos.length} saved {favoriteVideos.length === 1 ? 'video' : 'videos'} for instant access
        </p>
      </div>

      {favoriteVideos.length === 0 ? (
        <div className="nebula-card rounded-2xl p-10 text-center flex flex-col items-center gap-3">
          <div className="w-12 h-12 rounded-2xl nebula-accent-gradient flex items-center justify-center text-white">
            <Heart className="w-6 h-6" />
          </div>
          <h2 className="font-display font-semibold text-lg text-[var(--nebula-text)]">
            No favorites yet
          </h2>
          <p className="text-sm text-[var(--nebula-text-muted)] max-w-sm">
            Tap the heart icon on any video card or inside the player to pin it here.
          </p>
          <button
            type="button"
            onClick={() => onNavigate('library')}
            className="mt-2 min-h-[44px] px-5 py-2.5 rounded-xl nebula-accent-gradient text-white text-sm font-medium inline-flex items-center gap-2"
          >
            <Film className="w-4 h-4" />
            <span>Browse Library</span>
          </button>
        </div>
      ) : (
        <div
          className={
            layoutMode === 'list'
              ? 'flex flex-col gap-3'
              : 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4'
          }
        >
          {favoriteVideos.map((video) => (
            <VideoCard
              key={video.id}
              video={video}
              layout={layoutMode}
              isFavorite
              progress={progressMap[video.id]}
              animationLevel={animationLevel}
              onPlay={onPlay}
              onOpenDetails={onOpenDetails}
              onToggleFavorite={onToggleFavorite}
            />
          ))}
        </div>
      )}
    </div>
  );
};
