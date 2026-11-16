import { useCallback, useState } from 'react';
import { AnimatePresence, motion, MotionConfig } from 'motion/react';
import { INITIAL_VIDEOS } from './data/videos';
import { useNebulaState } from './hooks/usePreferences';
import { NavTab, VideoItem } from './types/nebula';
import { HomeView } from './components/HomeView';
import { LibraryView } from './components/LibraryView';
import { FavoritesView } from './components/FavoritesView';
import { PersonalizeView } from './components/PersonalizeView';
import { BottomNav } from './components/BottomNav';
import { ImmersivePlayer } from './components/ImmersivePlayer';
import { ResumeModal } from './components/ResumeModal';
import { VideoDetailsModal } from './components/VideoDetailsModal';

function extractLocalVideoMetadata(file: File): Promise<{
  duration: number;
  resolution: string;
  thumbnail: string;
  url: string;
}> {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(file);
    const vid = document.createElement('video');
    vid.preload = 'metadata';
    vid.muted = true;
    vid.playsInline = true;
    vid.src = url;

    const finish = (duration: number, resolution: string, thumbnail: string) => {
      resolve({ duration, resolution, thumbnail, url });
    };

    vid.onloadedmetadata = () => {
      const dur = Number.isFinite(vid.duration) && vid.duration > 0 ? vid.duration : 120;
      const res =
        vid.videoHeight >= 2160
          ? '4K'
          : vid.videoHeight >= 1080
          ? '1080p'
          : vid.videoHeight > 0
          ? `${vid.videoHeight}p`
          : 'HD';
      vid.currentTime = Math.min(1, dur * 0.1);
      vid.onseeked = () => {
        try {
          const canvas = document.createElement('canvas');
          canvas.width = 640;
          canvas.height = 360;
          const ctx = canvas.getContext('2d');
          if (ctx) {
            ctx.drawImage(vid, 0, 0, canvas.width, canvas.height);
            finish(dur, res, canvas.toDataURL('image/jpeg', 0.8));
            return;
          }
        } catch {
          // Ignore canvas capture issues
        }
        finish(dur, res, '');
      };
    };

    vid.onerror = () => {
      finish(120, 'Local', '');
    };
  });
}

export default function App() {
  const {
    preferences,
    updatePreferences,
    resetPreferences,
    favorites,
    toggleFavorite,
    progressMap,
    updateProgress,
    clearPlaybackHistory,
    recentIds,
    markRecentlyPlayed,
  } = useNebulaState();

  const [videos, setVideos] = useState<VideoItem[]>(INITIAL_VIDEOS);
  const [activeTab, setActiveTab] = useState<NavTab>('home');
  const [activeVideo, setActiveVideo] = useState<VideoItem | null>(null);
  const [playerStartPosition, setPlayerStartPosition] = useState(0);
  const [resumePrompt, setResumePrompt] = useState<{ video: VideoItem; position: number } | null>(null);
  const [detailsVideo, setDetailsVideo] = useState<VideoItem | null>(null);

  const launchPlayer = useCallback(
    (video: VideoItem, startPos: number) => {
      markRecentlyPlayed(video.id);
      setPlayerStartPosition(startPos);
      setActiveVideo(video);
      setDetailsVideo(null);
      setResumePrompt(null);
    },
    [markRecentlyPlayed]
  );

  const handlePlay = useCallback(
    (video: VideoItem, forceStartOver = false) => {
      if (forceStartOver || preferences.resumeBehavior === 'startOver') {
        launchPlayer(video, 0);
        return;
      }

      const saved = progressMap[video.id];
      const hasValidResume =
        saved && saved.position >= 5 && saved.position < (saved.duration || video.duration) - 5;

      if (!hasValidResume) {
        launchPlayer(video, 0);
        return;
      }

      if (preferences.resumeBehavior === 'always') {
        launchPlayer(video, saved.position);
      } else {
        setResumePrompt({ video, position: saved.position });
      }
    },
    [launchPlayer, preferences.resumeBehavior, progressMap]
  );

  const handleImportLocalFiles = useCallback(
    async (fileList: FileList) => {
      const files = Array.from(fileList).filter(
        (f) =>
          f.type.startsWith('video/') ||
          /\.(mp4|mkv|avi|mov|m4v|webm|flv|3gp|ts|mts|mpg|mpeg)$/i.test(f.name)
      );
      if (files.length === 0) return;

      const imported: VideoItem[] = [];
      for (const file of files) {
        const meta = await extractLocalVideoMetadata(file);
        const mb = (file.size / (1024 * 1024)).toFixed(1);
        const cleanTitle = file.name.replace(/\.[^/.]+$/, '').replace(/[_-]+/g, ' ');
        imported.push({
          id: `local-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
          title: cleanTitle || 'Local Video',
          subtitle: 'Imported from device storage',
          category: 'Local',
          duration: meta.duration,
          resolution: meta.resolution,
          sizeLabel: `${mb} MB`,
          thumbnail: meta.thumbnail,
          videoUrl: meta.url,
          visualTheme: 'cosmos',
          isLocal: true,
          addedAt: Date.now(),
          subtitles: [],
        });
      }

      if (imported.length > 0) {
        setVideos((prev) => [...imported, ...prev]);
        launchPlayer(imported[0], 0);
      }
    },
    [launchPlayer]
  );

  const activeIndex = activeVideo ? videos.findIndex((v) => v.id === activeVideo.id) : -1;

  const transitionSpec =
    preferences.animationLevel === 'none'
      ? { duration: 0 }
      : preferences.animationLevel === 'reduced'
      ? { duration: 0.12 }
      : { duration: 0.22, ease: [0.16, 1, 0.3, 1] as const };

  return (
    <MotionConfig
      reducedMotion={preferences.animationLevel === 'none' ? 'always' : 'user'}
    >
      <div className="relative min-h-screen bg-[var(--nebula-bg)] text-[var(--nebula-text)] transition-colors duration-200">
        <div
          aria-hidden="true"
          className="pointer-events-none fixed inset-x-0 top-0 h-80 opacity-45 dark:opacity-55"
          style={{
            background:
              'radial-gradient(ellipse 65% 55% at 50% -10%, rgba(var(--nebula-accent-rgb), 0.24), rgba(var(--nebula-accent-2-rgb), 0.1) 50%, transparent 80%)',
          }}
        />

        <main className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-6 sm:pt-8 pb-28">
          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab}
              initial={preferences.animationLevel === 'none' ? false : { opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={preferences.animationLevel === 'none' ? undefined : { opacity: 0, y: -6 }}
              transition={transitionSpec}
            >
              {activeTab === 'home' && (
                <HomeView
                  videos={videos}
                  preferences={preferences}
                  favorites={favorites}
                  progressMap={progressMap}
                  recentIds={recentIds}
                  onPlay={(v) => handlePlay(v)}
                  onOpenDetails={setDetailsVideo}
                  onToggleFavorite={toggleFavorite}
                  onLayoutChange={(layoutMode) => updatePreferences({ layoutMode })}
                  onNavigate={setActiveTab}
                  onImportLocalFiles={handleImportLocalFiles}
                  animationLevel={preferences.animationLevel}
                />
              )}

              {activeTab === 'library' && (
                <LibraryView
                  videos={videos}
                  layoutMode={preferences.layoutMode}
                  favorites={favorites}
                  progressMap={progressMap}
                  animationLevel={preferences.animationLevel}
                  onPlay={(v) => handlePlay(v)}
                  onOpenDetails={setDetailsVideo}
                  onToggleFavorite={toggleFavorite}
                  onLayoutChange={(layoutMode) => updatePreferences({ layoutMode })}
                  onImportLocalFiles={handleImportLocalFiles}
                />
              )}

              {activeTab === 'favorites' && (
                <FavoritesView
                  videos={videos}
                  favorites={favorites}
                  layoutMode={preferences.layoutMode}
                  progressMap={progressMap}
                  animationLevel={preferences.animationLevel}
                  onPlay={(v) => handlePlay(v)}
                  onOpenDetails={setDetailsVideo}
                  onToggleFavorite={toggleFavorite}
                  onNavigate={setActiveTab}
                />
              )}

              {activeTab === 'personalize' && (
                <PersonalizeView
                  preferences={preferences}
                  onUpdate={updatePreferences}
                  onReset={resetPreferences}
                  onClearHistory={clearPlaybackHistory}
                />
              )}
            </motion.div>
          </AnimatePresence>
        </main>

        {!activeVideo && (
          <BottomNav
            activeTab={activeTab}
            onChangeTab={setActiveTab}
            favoritesCount={favorites.length}
            animationLevel={preferences.animationLevel}
          />
        )}

        {detailsVideo && !activeVideo && (
          <VideoDetailsModal
            video={detailsVideo}
            progress={progressMap[detailsVideo.id]}
            isFavorite={favorites.includes(detailsVideo.id)}
            onPlay={(v, startOver) => handlePlay(v, startOver)}
            onToggleFavorite={toggleFavorite}
            onClose={() => setDetailsVideo(null)}
          />
        )}

        {resumePrompt && !activeVideo && (
          <ResumeModal
            video={resumePrompt.video}
            savedPosition={resumePrompt.position}
            onResume={() => launchPlayer(resumePrompt.video, resumePrompt.position)}
            onStartBeginning={() => launchPlayer(resumePrompt.video, 0)}
            onClose={() => setResumePrompt(null)}
          />
        )}

        <AnimatePresence>
          {activeVideo && (
            <ImmersivePlayer
              video={activeVideo}
              initialPosition={playerStartPosition}
              preferences={preferences}
              playerStyle={preferences.playerStyle}
              animationLevel={preferences.animationLevel}
              isFavorite={favorites.includes(activeVideo.id)}
              hasPrevious={activeIndex > 0}
              hasNext={activeIndex >= 0 && activeIndex < videos.length - 1}
              onPrevious={() => {
                if (activeIndex > 0) launchPlayer(videos[activeIndex - 1], 0);
              }}
              onNext={() => {
                if (activeIndex >= 0 && activeIndex < videos.length - 1) {
                  launchPlayer(videos[activeIndex + 1], 0);
                }
              }}
              onToggleFavorite={toggleFavorite}
              onProgress={updateProgress}
              onUpdatePreferences={updatePreferences}
              onClose={() => setActiveVideo(null)}
            />
          )}
        </AnimatePresence>
      </div>
    </MotionConfig>
  );
}
