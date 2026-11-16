import React, { useMemo, useState } from 'react';
import {
  ChevronRight,
  FolderPlus,
  LayoutGrid,
  List,
  Play,
  Search,
  Settings,
  X,
} from 'lucide-react';
import {
  AnimationLevel,
  LayoutMode,
  NavTab,
  PlaybackProgress,
  UserPreferences,
  VideoItem,
} from '../types/nebula';
import { formatTime } from '../data/videos';
import { ProfileAvatar } from './ProfileAvatar';
import { VideoCard } from './VideoCard';
import { VideoThumbnail } from './VideoThumbnail';
import { XivoxLogo } from './XivoxLogo';

interface HomeViewProps {
  videos: VideoItem[];
  preferences: UserPreferences;
  favorites: string[];
  progressMap: Record<string, PlaybackProgress>;
  recentIds: string[];
  onPlay: (video: VideoItem) => void;
  onOpenDetails: (video: VideoItem) => void;
  onToggleFavorite: (videoId: string) => void;
  onLayoutChange: (layout: LayoutMode) => void;
  onNavigate: (tab: NavTab) => void;
  onImportLocalFiles: (files: FileList) => void;
  animationLevel: AnimationLevel;
}

export const HomeView: React.FC<HomeViewProps> = ({
  videos,
  preferences,
  favorites,
  progressMap,
  recentIds,
  onPlay,
  onOpenDetails,
  onToggleFavorite,
  onLayoutChange,
  onNavigate,
  onImportLocalFiles,
  animationLevel,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');

  const categories = useMemo(() => {
    const set = new Set<string>(['All']);
    videos.forEach((v) => set.add(v.category));
    return Array.from(set);
  }, [videos]);

  const continueWatching = useMemo(() => {
    return videos
      .filter((v) => {
        const p = progressMap[v.id];
        return p && p.position >= 5 && p.position < p.duration - 5;
      })
      .sort((a, b) => (progressMap[b.id]?.updatedAt ?? 0) - (progressMap[a.id]?.updatedAt ?? 0));
  }, [videos, progressMap]);

  const recentlyPlayed = useMemo(() => {
    const map = new Map(videos.map((v) => [v.id, v]));
    return recentIds.map((id) => map.get(id)).filter((v): v is VideoItem => Boolean(v));
  }, [videos, recentIds]);

  const favoriteVideos = useMemo(
    () => videos.filter((v) => favorites.includes(v.id)),
    [videos, favorites]
  );

  const filteredLibrary = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return videos.filter((v) => {
      const matchesCat = selectedCategory === 'All' || v.category === selectedCategory;
      const matchesQuery =
        !q ||
        v.title.toLowerCase().includes(q) ||
        v.subtitle.toLowerCase().includes(q) ||
        v.category.toLowerCase().includes(q);
      return matchesCat && matchesQuery;
    });
  }, [videos, searchQuery, selectedCategory]);

  return (
    <div className="space-y-8">
      {/* Brand Header */}
      <section className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3 min-w-0">
          <XivoxLogo size={38} className="shrink-0 text-[var(--nebula-text)]" />
          <div className="min-w-0">
            <h1 className="font-display font-bold text-2xl sm:text-3xl tracking-tight text-[var(--nebula-text)] truncate">
              XIVOX
            </h1>
            <p className="text-xs font-medium text-[var(--nebula-accent)] tracking-wide">
              Beyond Playback.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 shrink-0">
          <label className="min-h-[44px] px-3.5 py-2 rounded-xl nebula-card nebula-border-glow text-xs font-medium text-[var(--nebula-text)] flex items-center gap-2 cursor-pointer transition-colors">
            <FolderPlus className="w-4 h-4 text-[var(--nebula-accent)]" />
            <span className="hidden sm:inline whitespace-nowrap">Open Video</span>
            <input
              type="file"
              accept="video/*,.mkv,.avi,.mov,.m4v,.webm,.flv,.3gp,.ts,.mts,.mpg,.mpeg"
              multiple
              className="sr-only"
              onChange={(e) => {
                if (e.target.files && e.target.files.length > 0) {
                  onImportLocalFiles(e.target.files);
                  e.target.value = '';
                }
              }}
            />
          </label>

          <button
            type="button"
            onClick={() => onNavigate('personalize')}
            aria-label="Open settings"
            className="min-h-[44px] min-w-[44px] rounded-xl nebula-card flex items-center justify-center text-[var(--nebula-text-muted)] hover:text-[var(--nebula-accent)] transition-colors"
          >
            <Settings className="w-5 h-5" />
          </button>

          <button
            type="button"
            onClick={() => onNavigate('personalize')}
            aria-label="Open personalization studio"
            className="rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--nebula-accent)] transition-transform active:scale-95"
          >
            <ProfileAvatar
              profileName={preferences.profileName}
              avatarId={preferences.avatarId}
              customAvatarUrl={preferences.customAvatarUrl}
              size="md"
            />
          </button>
        </div>
      </section>

      {/* Search Bar */}
      <section aria-label="Search videos">
        <div className="relative nebula-glass nebula-border-glow rounded-2xl transition-all">
          <Search className="w-5 h-5 text-[var(--nebula-accent)] absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search videos, formats, or folders..."
            className="w-full min-h-[50px] bg-transparent pl-12 pr-12 text-sm sm:text-base text-[var(--nebula-text)] placeholder:text-[var(--nebula-text-muted)] focus:outline-none rounded-2xl"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              aria-label="Clear search"
              className="min-h-[44px] min-w-[44px] flex items-center justify-center absolute right-1 top-1/2 -translate-y-1/2 text-[var(--nebula-text-muted)] hover:text-[var(--nebula-text)]"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      </section>

      {/* Continue Watching Shelf */}
      {!searchQuery && continueWatching.length > 0 && (
        <section aria-labelledby="continue-heading" className="space-y-3.5">
          <div className="flex items-center justify-between gap-4">
            <h2
              id="continue-heading"
              className="font-display font-semibold text-lg sm:text-xl text-[var(--nebula-text)]"
            >
              Continue Watching
            </h2>
            <span className="text-xs font-mono-tabular text-[var(--nebula-text-muted)]">
              {continueWatching.length} active
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {continueWatching.slice(0, 2).map((video) => {
              const prog = progressMap[video.id];
              const ratio = prog ? prog.position / prog.duration : 0;
              const remaining = prog ? Math.max(0, prog.duration - prog.position) : video.duration;
              return (
                <button
                  key={video.id}
                  type="button"
                  onClick={() => onPlay(video)}
                  className="group nebula-card nebula-border-glow rounded-2xl p-3.5 text-left flex flex-col sm:flex-row items-stretch sm:items-center gap-4 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--nebula-accent)]"
                >
                  <div className="sm:w-52 shrink-0">
                    <VideoThumbnail
                      src={video.thumbnail}
                      title={video.title}
                      duration={video.duration}
                      progressRatio={ratio}
                    />
                  </div>
                  <div className="flex-1 min-w-0 flex flex-col justify-between py-0.5">
                    <div>
                      <h3 className="font-display font-semibold text-base text-[var(--nebula-text)] truncate">
                        {video.title}
                      </h3>
                      <p className="text-xs text-[var(--nebula-text-muted)] line-clamp-2 mt-1">
                        {video.subtitle}
                      </p>
                    </div>
                    <div className="flex items-center justify-between gap-2 mt-3">
                      <span className="text-xs font-mono-tabular text-[var(--nebula-accent)] font-medium">
                        {formatTime(prog.position)} · {formatTime(remaining)} left
                      </span>
                      <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-[var(--nebula-text)] group-hover:text-[var(--nebula-accent)] transition-colors">
                        <Play className="w-3.5 h-3.5 fill-current" />
                        Resume
                      </span>
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </section>
      )}

      {/* Recently Added / Played Row */}
      {!searchQuery && recentlyPlayed.length > 0 && (
        <section aria-labelledby="recent-heading" className="space-y-3.5">
          <div className="flex items-center justify-between gap-4">
            <h2
              id="recent-heading"
              className="font-display font-semibold text-lg sm:text-xl text-[var(--nebula-text)]"
            >
              Recently Added
            </h2>
            <button
              type="button"
              onClick={() => onNavigate('library')}
              className="min-h-[44px] inline-flex items-center gap-1 text-xs font-medium text-[var(--nebula-accent)] hover:underline whitespace-nowrap"
            >
              <span>View all</span>
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          <div className="flex gap-4 overflow-x-auto scrollbar-none pb-1 -mx-1 px-1">
            {recentlyPlayed.map((video) => {
              const prog = progressMap[video.id];
              const ratio = prog ? prog.position / prog.duration : undefined;
              return (
                <button
                  key={video.id}
                  type="button"
                  onClick={() => onPlay(video)}
                  className="group w-60 sm:w-64 shrink-0 nebula-card nebula-border-glow rounded-2xl p-2.5 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--nebula-accent)]"
                >
                  <VideoThumbnail
                    src={video.thumbnail}
                    title={video.title}
                    duration={video.duration}
                    progressRatio={ratio}
                  />
                  <h3 className="font-display font-semibold text-sm text-[var(--nebula-text)] truncate mt-2.5 px-0.5">
                    {video.title}
                  </h3>
                  <p className="text-xs text-[var(--nebula-text-muted)] truncate mt-0.5 px-0.5">
                    {video.category} · {video.resolution}
                  </p>
                </button>
              );
            })}
          </div>
        </section>
      )}

      {/* Favorites Preview */}
      {!searchQuery && favoriteVideos.length > 0 && (
        <section aria-labelledby="favorites-preview-heading" className="space-y-3.5">
          <div className="flex items-center justify-between gap-4">
            <h2
              id="favorites-preview-heading"
              className="font-display font-semibold text-lg sm:text-xl text-[var(--nebula-text)]"
            >
              Favorites
            </h2>
            <button
              type="button"
              onClick={() => onNavigate('favorites')}
              className="min-h-[44px] inline-flex items-center gap-1 text-xs font-medium text-[var(--nebula-accent)] hover:underline whitespace-nowrap"
            >
              <span>Manage ({favoriteVideos.length})</span>
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {favoriteVideos.slice(0, 3).map((video) => (
              <VideoCard
                key={video.id}
                video={video}
                layout="grid"
                isFavorite
                progress={progressMap[video.id]}
                animationLevel={animationLevel}
                onPlay={onPlay}
                onOpenDetails={onOpenDetails}
                onToggleFavorite={onToggleFavorite}
              />
            ))}
          </div>
        </section>
      )}

      {/* All Videos Library */}
      <section aria-labelledby="all-videos-heading" className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2
            id="all-videos-heading"
            className="font-display font-semibold text-lg sm:text-xl text-[var(--nebula-text)]"
          >
            {searchQuery ? `Results (${filteredLibrary.length})` : 'All Videos'}
          </h2>

          <div className="flex items-center gap-1 p-1 rounded-xl nebula-card">
            <button
              type="button"
              onClick={() => onLayoutChange('grid')}
              aria-label="Grid layout"
              className={`min-h-[38px] min-w-[38px] rounded-lg flex items-center justify-center transition-colors ${
                preferences.layoutMode === 'grid'
                  ? 'nebula-accent-gradient text-white'
                  : 'text-[var(--nebula-text-muted)] hover:text-[var(--nebula-text)]'
              }`}
            >
              <LayoutGrid className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={() => onLayoutChange('list')}
              aria-label="List layout"
              className={`min-h-[38px] min-w-[38px] rounded-lg flex items-center justify-center transition-colors ${
                preferences.layoutMode === 'list'
                  ? 'nebula-accent-gradient text-white'
                  : 'text-[var(--nebula-text-muted)] hover:text-[var(--nebula-text)]'
              }`}
            >
              <List className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Category Filter Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto scrollbar-none pb-1">
          {categories.map((cat) => {
            const active = selectedCategory === cat;
            return (
              <button
                key={cat}
                type="button"
                onClick={() => setSelectedCategory(cat)}
                className={`min-h-[40px] px-4 rounded-xl text-xs font-medium whitespace-nowrap shrink-0 transition-colors ${
                  active
                    ? 'nebula-accent-gradient text-white shadow-sm'
                    : 'nebula-card text-[var(--nebula-text-muted)] hover:text-[var(--nebula-text)]'
                }`}
              >
                {cat}
              </button>
            );
          })}
        </div>

        {filteredLibrary.length === 0 ? (
          <div className="nebula-card rounded-2xl p-10 text-center space-y-2">
            <p className="font-display font-semibold text-base text-[var(--nebula-text)]">
              No matching videos found
            </p>
            <p className="text-sm text-[var(--nebula-text-muted)]">
              Try a different keyword or open a local video file from your device.
            </p>
          </div>
        ) : (
          <div
            className={
              preferences.layoutMode === 'list'
                ? 'flex flex-col gap-3'
                : 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4'
            }
          >
            {filteredLibrary.map((video) => (
              <VideoCard
                key={video.id}
                video={video}
                layout={preferences.layoutMode}
                isFavorite={favorites.includes(video.id)}
                progress={progressMap[video.id]}
                animationLevel={animationLevel}
                onPlay={onPlay}
                onOpenDetails={onOpenDetails}
                onToggleFavorite={onToggleFavorite}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
};
