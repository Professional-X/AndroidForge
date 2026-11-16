import React, { useState } from 'react';
import {
  Captions,
  Check,
  LayoutGrid,
  List,
  Lock,
  Monitor,
  Moon,
  RotateCcw,
  Sliders,
  Sparkles,
  Sun,
  Trash2,
  Upload,
} from 'lucide-react';
import { ACCENT_PRESETS, AVATAR_OPTIONS } from '../data/videos';
import {
  AnimationLevel,
  LayoutMode,
  PlayerControlStyle,
  ResumeBehavior,
  ThemeMode,
  UserPreferences,
} from '../types/nebula';
import { ProfileAvatar } from './ProfileAvatar';
import { XivoxLogo } from './XivoxLogo';

interface PersonalizeViewProps {
  preferences: UserPreferences;
  onUpdate: (patch: Partial<UserPreferences>) => void;
  onReset: () => void;
  onClearHistory: () => void;
}

const THEME_OPTIONS: { id: ThemeMode; label: string; desc: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { id: 'amoled', label: 'AMOLED Black', desc: 'True #000000 OLED canvas', icon: Moon },
  { id: 'dark', label: 'Midnight Dark', desc: 'Deep slate contrast', icon: Monitor },
  { id: 'light', label: 'Crisp Light', desc: 'Daylight studio mode', icon: Sun },
];

const PLAYER_STYLES: { id: PlayerControlStyle; label: string; desc: string }[] = [
  { id: 'glass', label: 'Floating Glass', desc: 'Translucent dock with full controls' },
  { id: 'minimal', label: 'Minimal Cinema', desc: 'Slim edge bar for pure frame focus' },
  { id: 'orbital', label: 'Orbital Dock', desc: 'Centered radial transport ring' },
];

const ANIMATION_LEVELS: { id: AnimationLevel; label: string }[] = [
  { id: 'full', label: 'Full Motion' },
  { id: 'reduced', label: 'Subtle' },
  { id: 'none', label: 'Instant' },
];

const RESUME_MODES: { id: ResumeBehavior; label: string; desc: string }[] = [
  { id: 'ask', label: 'Ask Every Time', desc: 'Show Resume from MM:SS? dialog' },
  { id: 'always', label: 'Always Resume', desc: 'Jump directly to saved position' },
  { id: 'startOver', label: 'Start Over', desc: 'Always play from 00:00' },
];

const SUBTITLE_COLORS = [
  { label: 'White', value: '#FFFFFF' },
  { label: 'Cyan', value: '#00E5FF' },
  { label: 'Gold', value: '#FACC15' },
  { label: 'Silver', value: '#CBD5E1' },
];

export const PersonalizeView: React.FC<PersonalizeViewProps> = ({
  preferences,
  onUpdate,
  onReset,
  onClearHistory,
}) => {
  const [savedNotice, setSavedNotice] = useState('');

  const notify = (msg: string) => {
    setSavedNotice(msg);
    window.setTimeout(() => setSavedNotice(''), 1800);
  };

  const handleUpdate = (patch: Partial<UserPreferences>) => {
    onUpdate(patch);
    notify('Preferences saved locally');
  };

  const handleCustomAvatar = (file: File) => {
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        handleUpdate({ avatarId: 'custom', customAvatarUrl: reader.result });
      }
    };
    reader.readAsDataURL(file);
  };

  return (
    <div className="space-y-7 max-w-4xl">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="font-display font-bold text-2xl sm:text-3xl tracking-tight text-[var(--nebula-text)]">
            Settings & Studio
          </h1>
          <p className="text-sm text-[var(--nebula-text-muted)] mt-0.5">
            {savedNotice || 'All preferences and playback history persist locally on this device'}
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            onReset();
            notify('Restored default settings');
          }}
          className="min-h-[44px] px-4 py-2 rounded-xl nebula-card text-xs font-medium text-[var(--nebula-text)] hover:border-[var(--nebula-accent)] inline-flex items-center gap-2 transition-colors"
        >
          <RotateCcw className="w-4 h-4" />
          <span>Reset Defaults</span>
        </button>
      </div>

      {/* Profile Identity */}
      <section className="nebula-card rounded-2xl p-5 space-y-5">
        <h2 className="font-display font-semibold text-base text-[var(--nebula-text)]">
          Profile Identity
        </h2>

        <div className="flex flex-col sm:flex-row sm:items-center gap-5">
          <ProfileAvatar
            profileName={preferences.profileName}
            avatarId={preferences.avatarId}
            customAvatarUrl={preferences.customAvatarUrl}
            size="lg"
          />

          <div className="flex-1 space-y-2">
            <label
              htmlFor="nebula-profile-name"
              className="block text-xs font-medium text-[var(--nebula-text-muted)]"
            >
              Display Name
            </label>
            <input
              id="nebula-profile-name"
              type="text"
              maxLength={32}
              value={preferences.profileName}
              onChange={(e) => handleUpdate({ profileName: e.target.value })}
              placeholder="Your name"
              className="w-full max-w-sm min-h-[44px] px-3.5 rounded-xl bg-black/5 dark:bg-white/5 border border-[var(--nebula-border)] text-sm text-[var(--nebula-text)] focus:outline-none focus:border-[var(--nebula-accent)]"
            />
          </div>
        </div>

        <div className="space-y-2.5">
          <p className="text-xs font-medium text-[var(--nebula-text-muted)]">Choose Avatar</p>
          <div className="flex flex-wrap items-center gap-3">
            {AVATAR_OPTIONS.map((opt) => {
              const selected = preferences.avatarId === opt.id;
              return (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => handleUpdate({ avatarId: opt.id })}
                  className={`min-h-[44px] px-3 py-2 rounded-xl border flex items-center gap-2.5 text-xs font-medium transition-all ${
                    selected
                      ? 'border-[var(--nebula-accent)] bg-[rgba(var(--nebula-accent-rgb),0.14)] text-[var(--nebula-text)]'
                      : 'border-[var(--nebula-border)] text-[var(--nebula-text-muted)] hover:text-[var(--nebula-text)]'
                  }`}
                >
                  <ProfileAvatar
                    profileName={preferences.profileName}
                    avatarId={opt.id}
                    size="sm"
                  />
                  <span>{opt.label}</span>
                </button>
              );
            })}

            <label className="min-h-[44px] px-3.5 py-2 rounded-xl border border-dashed border-[var(--nebula-border)] hover:border-[var(--nebula-accent)] flex items-center gap-2 text-xs font-medium text-[var(--nebula-text)] cursor-pointer transition-colors">
              <Upload className="w-4 h-4 text-[var(--nebula-accent)]" />
              <span>Upload Photo</span>
              <input
                type="file"
                accept="image/*"
                className="sr-only"
                onChange={(e) => {
                  const f = e.target.files?.[0];
                  if (f) handleCustomAvatar(f);
                }}
              />
            </label>
          </div>
        </div>
      </section>

      {/* Theme Mode & Accent Spectrum */}
      <section className="nebula-card rounded-2xl p-5 space-y-4">
        <h2 className="font-display font-semibold text-base text-[var(--nebula-text)]">
          Surface Theme & Accent
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {THEME_OPTIONS.map((theme) => {
            const Icon = theme.icon;
            const active = preferences.theme === theme.id;
            return (
              <button
                key={theme.id}
                type="button"
                onClick={() => handleUpdate({ theme: theme.id })}
                className={`min-h-[68px] p-3.5 rounded-xl border text-left flex items-start gap-3 transition-all ${
                  active
                    ? 'border-[var(--nebula-accent)] bg-[rgba(var(--nebula-accent-rgb),0.12)]'
                    : 'border-[var(--nebula-border)] hover:border-[rgba(var(--nebula-accent-rgb),0.4)]'
                }`}
              >
                <div className="w-9 h-9 rounded-lg nebula-accent-gradient flex items-center justify-center text-white shrink-0">
                  <Icon className="w-4 h-4" />
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-[var(--nebula-text)]">{theme.label}</p>
                  <p className="text-xs text-[var(--nebula-text-muted)] mt-0.5">{theme.desc}</p>
                </div>
              </button>
            );
          })}
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 pt-2">
          {ACCENT_PRESETS.map((preset) => {
            const active = preferences.accent === preset.id;
            return (
              <button
                key={preset.id}
                type="button"
                onClick={() => handleUpdate({ accent: preset.id })}
                className={`min-h-[56px] p-3 rounded-xl border flex items-center gap-2.5 text-left transition-all ${
                  active
                    ? 'border-[var(--nebula-accent)] bg-[rgba(var(--nebula-accent-rgb),0.12)]'
                    : 'border-[var(--nebula-border)] hover:border-[rgba(var(--nebula-accent-rgb),0.4)]'
                }`}
              >
                <span
                  className="w-6 h-6 rounded-full shrink-0 flex items-center justify-center text-white"
                  style={{
                    background: `linear-gradient(135deg, ${preset.primary}, ${preset.secondary})`,
                  }}
                >
                  {active && <Check className="w-3.5 h-3.5" />}
                </span>
                <span className="text-xs font-medium text-[var(--nebula-text)] truncate">
                  {preset.label}
                </span>
              </button>
            );
          })}
        </div>
      </section>

      {/* Layout & Motion */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <section className="nebula-card rounded-2xl p-5 space-y-4">
          <h2 className="font-display font-semibold text-base text-[var(--nebula-text)]">
            Library Layout
          </h2>
          <div className="grid grid-cols-2 gap-3">
            {(
              [
                { id: 'grid' as LayoutMode, label: 'Grid Cards', icon: LayoutGrid },
                { id: 'list' as LayoutMode, label: 'List Rows', icon: List },
              ]
            ).map((opt) => {
              const Icon = opt.icon;
              const active = preferences.layoutMode === opt.id;
              return (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => handleUpdate({ layoutMode: opt.id })}
                  className={`min-h-[48px] px-4 py-3 rounded-xl border flex items-center justify-center gap-2 text-xs font-semibold transition-all ${
                    active
                      ? 'nebula-accent-gradient text-white border-transparent'
                      : 'border-[var(--nebula-border)] text-[var(--nebula-text)]'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{opt.label}</span>
                </button>
              );
            })}
          </div>
        </section>

        <section className="nebula-card rounded-2xl p-5 space-y-4">
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-[var(--nebula-accent)]" />
            <h2 className="font-display font-semibold text-base text-[var(--nebula-text)]">
              Animation Preference
            </h2>
          </div>
          <div className="grid grid-cols-3 gap-2">
            {ANIMATION_LEVELS.map((lvl) => {
              const active = preferences.animationLevel === lvl.id;
              return (
                <button
                  key={lvl.id}
                  type="button"
                  onClick={() => handleUpdate({ animationLevel: lvl.id })}
                  className={`min-h-[48px] px-2 rounded-xl border text-xs font-semibold transition-all ${
                    active
                      ? 'nebula-accent-gradient text-white border-transparent'
                      : 'border-[var(--nebula-border)] text-[var(--nebula-text)]'
                  }`}
                >
                  {lvl.label}
                </button>
              );
            })}
          </div>
        </section>
      </div>

      {/* Player & Resume Behavior */}
      <section className="nebula-card rounded-2xl p-5 space-y-5">
        <h2 className="font-display font-semibold text-base text-[var(--nebula-text)]">
          Player Interface & Continue Watching
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {PLAYER_STYLES.map((style) => {
            const active = preferences.playerStyle === style.id;
            return (
              <button
                key={style.id}
                type="button"
                onClick={() => handleUpdate({ playerStyle: style.id })}
                className={`min-h-[68px] p-3.5 rounded-xl border text-left transition-all ${
                  active
                    ? 'border-[var(--nebula-accent)] bg-[rgba(var(--nebula-accent-rgb),0.12)]'
                    : 'border-[var(--nebula-border)] hover:border-[rgba(var(--nebula-accent-rgb),0.4)]'
                }`}
              >
                <p className="text-sm font-semibold text-[var(--nebula-text)]">{style.label}</p>
                <p className="text-xs text-[var(--nebula-text-muted)] mt-0.5">{style.desc}</p>
              </button>
            );
          })}
        </div>

        <div className="space-y-2.5 pt-2 border-t border-[var(--nebula-border)]">
          <p className="text-xs font-medium text-[var(--nebula-text-muted)]">Resume Playback Behavior</p>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {RESUME_MODES.map((mode) => {
              const active = preferences.resumeBehavior === mode.id;
              return (
                <button
                  key={mode.id}
                  type="button"
                  onClick={() => handleUpdate({ resumeBehavior: mode.id })}
                  className={`min-h-[62px] p-3 rounded-xl border text-left transition-all ${
                    active
                      ? 'border-[var(--nebula-accent)] bg-[rgba(var(--nebula-accent-rgb),0.12)]'
                      : 'border-[var(--nebula-border)]'
                  }`}
                >
                  <p className="text-xs font-semibold text-[var(--nebula-text)]">{mode.label}</p>
                  <p className="text-[11px] text-[var(--nebula-text-muted)] mt-0.5">{mode.desc}</p>
                </button>
              );
            })}
          </div>
        </div>

        <div className="pt-2 border-t border-[var(--nebula-border)] flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-[var(--nebula-text)]">Default Playback Speed</p>
            <p className="text-xs text-[var(--nebula-text-muted)] font-mono-tabular">
              {preferences.defaultSpeed}x
            </p>
          </div>
          <div className="flex items-center gap-1.5 flex-wrap">
            {[0.75, 1, 1.25, 1.5, 2].map((spd) => (
              <button
                key={spd}
                type="button"
                onClick={() => handleUpdate({ defaultSpeed: spd })}
                className={`min-h-[38px] px-3 rounded-lg border text-xs font-mono-tabular font-semibold ${
                  preferences.defaultSpeed === spd
                    ? 'nebula-accent-gradient text-white border-transparent'
                    : 'border-[var(--nebula-border)] text-[var(--nebula-text)]'
                }`}
              >
                {spd}x
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* Subtitle Preferences */}
      <section className="nebula-card rounded-2xl p-5 space-y-5">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-2.5">
            <Captions className="w-5 h-5 text-[var(--nebula-accent)]" />
            <div>
              <h2 className="font-display font-semibold text-base text-[var(--nebula-text)]">
                Subtitle Preferences
              </h2>
              <p className="text-xs text-[var(--nebula-text-muted)]">
                Supports offline SRT, VTT, and ASS/SSA tracks
              </p>
            </div>
          </div>

          <button
            type="button"
            role="switch"
            aria-checked={preferences.subtitlesEnabled}
            onClick={() => handleUpdate({ subtitlesEnabled: !preferences.subtitlesEnabled })}
            className={`w-12 h-7 rounded-full p-1 transition-colors ${
              preferences.subtitlesEnabled ? 'nebula-accent-gradient' : 'bg-slate-600/40'
            }`}
          >
            <span
              className={`block w-5 h-5 rounded-full bg-white shadow transition-transform ${
                preferences.subtitlesEnabled ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2 border-t border-[var(--nebula-border)]">
          <div className="space-y-2">
            <label className="text-xs font-medium text-[var(--nebula-text-muted)] flex justify-between">
              <span>Font Size</span>
              <span className="font-mono-tabular text-[var(--nebula-text)]">{preferences.subtitleFontSize}px</span>
            </label>
            <input
              type="range"
              min={12}
              max={28}
              value={preferences.subtitleFontSize}
              onChange={(e) => handleUpdate({ subtitleFontSize: Number(e.target.value) })}
              className="w-full accent-[var(--nebula-accent)]"
            />
          </div>

          <div className="space-y-2">
            <label className="text-xs font-medium text-[var(--nebula-text-muted)] flex justify-between">
              <span>Background Opacity</span>
              <span className="font-mono-tabular text-[var(--nebula-text)]">
                {Math.round(preferences.subtitleBgOpacity * 100)}%
              </span>
            </label>
            <input
              type="range"
              min={0}
              max={1}
              step={0.05}
              value={preferences.subtitleBgOpacity}
              onChange={(e) => handleUpdate({ subtitleBgOpacity: Number(e.target.value) })}
              className="w-full accent-[var(--nebula-accent)]"
            />
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs text-[var(--nebula-text-muted)] mr-2">Text Color:</span>
          {SUBTITLE_COLORS.map((c) => (
            <button
              key={c.value}
              type="button"
              onClick={() => handleUpdate({ subtitleColor: c.value })}
              className={`min-h-[38px] px-3 rounded-lg border text-xs font-medium flex items-center gap-2 ${
                preferences.subtitleColor === c.value
                  ? 'border-[var(--nebula-accent)] bg-[rgba(var(--nebula-accent-rgb),0.12)] text-[var(--nebula-text)]'
                  : 'border-[var(--nebula-border)] text-[var(--nebula-text-muted)]'
              }`}
            >
              <span className="w-3 h-3 rounded-full border border-white/20" style={{ backgroundColor: c.value }} />
              <span>{c.label}</span>
            </button>
          ))}
        </div>
      </section>

      {/* Gesture Controls */}
      <section className="nebula-card rounded-2xl p-5 space-y-4">
        <div className="flex items-center gap-2">
          <Sliders className="w-4 h-4 text-[var(--nebula-accent)]" />
          <h2 className="font-display font-semibold text-base text-[var(--nebula-text)]">
            Gesture Controls
          </h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {(
            [
              { key: 'doubleTapSeekEnabled', label: 'Double-Tap Seek (-10s / +10s)' },
              { key: 'brightnessSwipeEnabled', label: 'Left Vertical Swipe Brightness' },
              { key: 'volumeSwipeEnabled', label: 'Right Vertical Swipe Volume' },
              { key: 'horizontalSeekEnabled', label: 'Horizontal Swipe Scrubbing' },
              { key: 'longPress2xEnabled', label: 'Long-Press 2.0x Temporary Speed' },
            ] as const
          ).map((item) => {
            const enabled = preferences[item.key];
            return (
              <button
                key={item.key}
                type="button"
                onClick={() => handleUpdate({ [item.key]: !enabled })}
                className="min-h-[48px] px-3.5 py-2.5 rounded-xl border border-[var(--nebula-border)] flex items-center justify-between gap-3 text-left"
              >
                <span className="text-xs font-medium text-[var(--nebula-text)]">{item.label}</span>
                <span
                  className={`text-[11px] font-mono-tabular font-semibold ${
                    enabled ? 'text-[var(--nebula-accent)]' : 'text-[var(--nebula-text-muted)]'
                  }`}
                >
                  {enabled ? 'ON' : 'OFF'}
                </span>
              </button>
            );
          })}
        </div>
      </section>

      {/* History & App Information */}
      <section className="nebula-card rounded-2xl p-5 space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <XivoxLogo size={36} className="text-[var(--nebula-text)]" />
            <div>
              <h2 className="font-display font-semibold text-base text-[var(--nebula-text)]">
                XIVOX Video Player · Beyond Playback.
              </h2>
              <p className="text-xs text-[var(--nebula-text-muted)] flex items-center gap-1.5 mt-0.5">
                <Lock className="w-3.5 h-3.5 text-[var(--nebula-accent)]" />
                <span>Offline & private: zero login, zero ads, zero telemetry.</span>
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              onClearHistory();
              notify('Playback history cleared');
            }}
            className="min-h-[44px] px-4 py-2 rounded-xl border border-rose-500/40 text-rose-400 hover:bg-rose-500/10 text-xs font-semibold inline-flex items-center gap-2 transition-colors"
          >
            <Trash2 className="w-4 h-4" />
            <span>Clear Playback History</span>
          </button>
        </div>
      </section>
    </div>
  );
};
