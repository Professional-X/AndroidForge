export type ThemeMode = 'amoled' | 'dark' | 'light';

export type AccentPresetId = 'nebula' | 'cyan' | 'amber' | 'emerald' | 'crimson';

export type LayoutMode = 'grid' | 'list';

export type AnimationLevel = 'full' | 'reduced' | 'none';

export type PlayerControlStyle = 'glass' | 'minimal' | 'orbital';

export type NavTab = 'home' | 'library' | 'favorites' | 'personalize';

export type ResumeBehavior = 'ask' | 'always' | 'startOver';

export interface SubtitleCue {
  start: number;
  end: number;
  text: string;
}

export interface VideoItem {
  id: string;
  title: string;
  subtitle: string;
  category: string;
  duration: number; // seconds
  resolution: string;
  sizeLabel: string;
  thumbnail: string;
  videoUrl: string;
  subtitles: SubtitleCue[];
  visualTheme: 'cosmos' | 'city' | 'aurora' | 'drive' | 'ocean';
  isLocal?: boolean;
  addedAt: number;
}

export interface PlaybackProgress {
  position: number;
  duration: number;
  updatedAt: number;
}

export interface UserPreferences {
  profileName: string;
  avatarId: string;
  customAvatarUrl?: string;
  theme: ThemeMode;
  accent: AccentPresetId;
  layoutMode: LayoutMode;
  animationLevel: AnimationLevel;
  playerStyle: PlayerControlStyle;
  defaultSpeed: number;
  subtitlesEnabled: boolean;
  resumeBehavior: ResumeBehavior;
  subtitleFontSize: number;
  subtitleColor: string;
  subtitleBgOpacity: number;
  subtitlePositionBottom: number;
  subtitleDelayMs: number;
  doubleTapSeekEnabled: boolean;
  brightnessSwipeEnabled: boolean;
  volumeSwipeEnabled: boolean;
  horizontalSeekEnabled: boolean;
  longPress2xEnabled: boolean;
}
