import React, { useMemo, useState } from 'react';
import { ArrowUpDown, FolderPlus, LayoutGrid, List, Search, X } from 'lucide-react';
import {
  AnimationLevel,
  LayoutMode,
  PlaybackProgress,
  VideoItem,
} from '../types/nebula';
import { VideoCard } from './VideoCard';

interface LibraryViewProps {
  videos: VideoItem[];
  layoutMode: LayoutMode;
  favorites: string[];
  progressMap: Record<string, PlaybackProgress>;
  animationLevel: AnimationLevel;
  onPlay: (video: VideoItem) => void;
  onOpenDetails: (video: VideoItem) => void;
  onToggleFavorite: (videoId: string) => void;
  onLayoutChange: (layout: LayoutMode) => void;
  onImportLocalFiles: (files: FileList) => void;
}

type SortKey = 'recent' | 'title' | 'duration' | 'size';

function parseSizeMb(sizeLabel: string): number {
  const num = parseFloat(sizeLabel);
  if (Number.isNaN(num)) return 0;
  return sizeLabel.toUpperCase().includes('GB') ? num * 1024 : num;
}

export const LibraryView: React.FC<LibraryViewProps> = ({
  videos,
  layoutMode,
  favorites,
  progressMap,
  animationLevel,
  onPlay,
  onOpenDetails,
  onToggleFavorite,
  onLayoutChange,
  onImportLocalFiles,
}) => {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('All');
  const [sortKey, setSortKey] = useState<SortKey>('recent');

  const categories = useMemo(() => {
    const set = new Set<string>(['All']);
    videos.forEach((v) => set.add(v.category));
    return Array.from(set);
  }, [videos]);

  const sortedFiltered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const list = videos.filter((v) => {
      const catMatch = category === 'All' || v.category === category;
      const queryMatch =
        !q ||
        v.title.toLowerCase().includes(q) ||
        v.subtitle.toLowerCase().includes(q) ||
        v.category.toLowerCase().includes(q);
      return catMatch && queryMatch;
    });

    return [...list].sort((a, b) => {
      if (sortKey === 'title') return a.title.localeCompare(b.title);
      if (sortKey === 'duration') return b.duration - a.duration;
      if (sortKey === 'size') return parseSizeMb(b.sizeLabel) - parseSizeMb(a.sizeLabel);
      return b.addedAt - a.addedAt;
    });
  }, [videos, query, category, sortKey]);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="font-display font-bold text-2xl sm:text-3xl tracking-tight text-[var(--nebula-text)]">
            Video Library
          </h1>
          <p className="text-sm text-[var(--nebula-text-muted)] mt-0.5">
            {videos.length} videos ready for offline playback
          </p>
        </div>

        <label className="min-h-[44px] px-4 py-2 rounded-xl nebula-accent-gradient nebula-accent-glow text-white text-xs sm:text-sm font-medium inline-flex items-center gap-2 cursor-pointer active:scale-95 transition-transform">
          <FolderPlus className="w-4 h-4" />
          <span className="whitespace-nowrap">Open Local Video</span>
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
      </div>

      {/* Search & Controls */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1 nebula-glass nebula-border-glow rounded-2xl">
          <Search className="w-4 h-4 text-[var(--nebula-accent)] absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Filter library..."
            className="w-full min-h-[46px] bg-transparent pl-11 pr-11 text-sm text-[var(--nebula-text)] placeholder:text-[var(--nebula-text-muted)] focus:outline-none rounded-2xl"
          />
          {query && (
            <button
              type="button"
              onClick={() => setQuery('')}
              aria-label="Clear filter"
              className="min-h-[44px] min-w-[44px] flex items-center justify-center absolute right-0.5 top-1/2 -translate-y-1/2 text-[var(--nebula-text-muted)]"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        <div className="flex items-center gap-2">
          <div className="nebula-card rounded-xl px-3 min-h-[46px] flex items-center gap-2">
            <ArrowUpDown className="w-4 h-4 text-[var(--nebula-text-muted)] shrink-0" />
            <select
              value={sortKey}
              onChange={(e) => setSortKey(e.target.value as SortKey)}
              aria-label="Sort videos"
              className="bg-transparent text-xs sm:text-sm font-medium text-[var(--nebula-text)] focus:outline-none cursor-pointer"
            >
              <option value="recent" className="bg-slate-900 text-white">Date Added</option>
              <option value="title" className="bg-slate-900 text-white">Name A-Z</option>
              <option value="duration" className="bg-slate-900 text-white">Duration</option>
              <option value="size" className="bg-slate-900 text-white">File Size</option>
            </select>
          </div>

          <div className="flex items-center gap-1 p-1 rounded-xl nebula-card">
            <button
              type="button"
              onClick={() => onLayoutChange('grid')}
              aria-label="Grid layout"
              className={`min-h-[38px] min-w-[38px] rounded-lg flex items-center justify-center transition-colors ${
                layoutMode === 'grid'
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
                layoutMode === 'list'
                  ? 'nebula-accent-gradient text-white'
                  : 'text-[var(--nebula-text-muted)] hover:text-[var(--nebula-text)]'
              }`}
            >
              <List className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Folder / Category Filter */}
      <div className="flex items-center gap-2 overflow-x-auto scrollbar-none pb-1">
        {categories.map((cat) => (
          <button
            key={cat}
            type="button"
            onClick={() => setCategory(cat)}
            className={`min-h-[40px] px-4 rounded-xl text-xs font-medium whitespace-nowrap shrink-0 transition-colors ${
              category === cat
                ? 'nebula-accent-gradient text-white'
                : 'nebula-card text-[var(--nebula-text-muted)] hover:text-[var(--nebula-text)]'
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Library Grid/List */}
      {sortedFiltered.length === 0 ? (
        <div className="nebula-card rounded-2xl p-10 text-center space-y-2">
          <p className="font-display font-semibold text-base text-[var(--nebula-text)]">
            No videos match this filter
          </p>
          <p className="text-sm text-[var(--nebula-text-muted)]">
            Reset your search or import local files from your device.
          </p>
        </div>
      ) : (
        <div
          className={
            layoutMode === 'list'
              ? 'flex flex-col gap-3'
              : 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4'
          }
        >
          {sortedFiltered.map((video) => (
            <VideoCard
              key={video.id}
              video={video}
              layout={layoutMode}
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
    </div>
  );
};
