import { useEffect, useState, useCallback } from 'react';
import { PlaybackProgress, UserPreferences } from '../types/nebula';

const PREFS_KEY = 'xivox_player_prefs_v2';
const FAVORITES_KEY = 'xivox_player_favorites_v2';
const PROGRESS_KEY = 'xivox_player_progress_v2';
const RECENT_KEY = 'xivox_player_recent_v2';

export const DEFAULT_PREFERENCES: UserPreferences = {
  profileName: 'Aria Vance',
  avatarId: 'pilot',
  customAvatarUrl: undefined,
  theme: 'amoled',
  accent: 'nebula',
  layoutMode: 'grid',
  animationLevel: 'full',
  playerStyle: 'glass',
  defaultSpeed: 1,
  subtitlesEnabled: true,
  resumeBehavior: 'ask',
  subtitleFontSize: 16,
  subtitleColor: '#FFFFFF',
  subtitleBgOpacity: 0.78,
  subtitlePositionBottom: 14,
  subtitleDelayMs: 0,
  doubleTapSeekEnabled: true,
  brightnessSwipeEnabled: true,
  volumeSwipeEnabled: true,
  horizontalSeekEnabled: true,
  longPress2xEnabled: true,
};

function safeReadJson<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

function safeWriteJson<T>(key: string, value: T): void {
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Ignore storage quota issues in restricted browsers
  }
}

export function useNebulaState() {
  const [preferences, setPreferences] = useState<UserPreferences>(() => ({
    ...DEFAULT_PREFERENCES,
    ...safeReadJson<Partial<UserPreferences>>(PREFS_KEY, {}),
  }));

  const [favorites, setFavorites] = useState<string[]>(() =>
    safeReadJson<string[]>(FAVORITES_KEY, ['vid-cosmos-drift', 'vid-abyssal-drift'])
  );

  const [progressMap, setProgressMap] = useState<Record<string, PlaybackProgress>>(() =>
    safeReadJson<Record<string, PlaybackProgress>>(PROGRESS_KEY, {
      'vid-cosmos-drift': { position: 98, duration: 254, updatedAt: Date.now() - 3600000 },
      'vid-glacial-aurora': { position: 142, duration: 312, updatedAt: Date.now() - 7200000 },
    })
  );

  const [recentIds, setRecentIds] = useState<string[]>(() =>
    safeReadJson<string[]>(RECENT_KEY, [
      'vid-cosmos-drift',
      'vid-glacial-aurora',
      'vid-midnight-metropolis',
    ])
  );

  useEffect(() => {
    const root = document.documentElement;
    root.setAttribute('data-theme', preferences.theme);
    root.setAttribute('data-accent', preferences.accent);
    if (preferences.theme === 'light') {
      root.classList.remove('dark');
    } else {
      root.classList.add('dark');
    }
    safeWriteJson(PREFS_KEY, preferences);
  }, [preferences]);

  useEffect(() => {
    safeWriteJson(FAVORITES_KEY, favorites);
  }, [favorites]);

  useEffect(() => {
    safeWriteJson(PROGRESS_KEY, progressMap);
  }, [progressMap]);

  useEffect(() => {
    safeWriteJson(RECENT_KEY, recentIds);
  }, [recentIds]);

  const updatePreferences = useCallback((patch: Partial<UserPreferences>) => {
    setPreferences((prev) => ({ ...prev, ...patch }));
  }, []);

  const resetPreferences = useCallback(() => {
    setPreferences(DEFAULT_PREFERENCES);
  }, []);

  const toggleFavorite = useCallback((videoId: string) => {
    setFavorites((prev) =>
      prev.includes(videoId) ? prev.filter((id) => id !== videoId) : [videoId, ...prev]
    );
  }, []);

  const updateProgress = useCallback((videoId: string, position: number, duration: number) => {
    if (!Number.isFinite(position) || !Number.isFinite(duration) || duration <= 0) return;
    setProgressMap((prev) => ({
      ...prev,
      [videoId]: {
        position: Math.min(Math.max(0, position), duration),
        duration,
        updatedAt: Date.now(),
      },
    }));
  }, []);

  const clearPlaybackHistory = useCallback(() => {
    setProgressMap({});
    setRecentIds([]);
  }, []);

  const markRecentlyPlayed = useCallback((videoId: string) => {
    setRecentIds((prev) => [videoId, ...prev.filter((id) => id !== videoId)].slice(0, 12));
  }, []);

  return {
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
  };
}
