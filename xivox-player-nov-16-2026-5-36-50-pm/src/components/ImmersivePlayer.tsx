import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  Captions,
  FastForward,
  FileText,
  Gauge,
  Heart,
  Lock,
  Maximize,
  Minimize,
  Pause,
  PictureInPicture2,
  Play,
  Rewind,
  SkipBack,
  SkipForward,
  Sun,
  Unlock,
  Volume2,
  VolumeX,
} from 'lucide-react';
import { AnimatePresence, motion } from 'motion/react';
import {
  AnimationLevel,
  PlayerControlStyle,
  SubtitleCue,
  UserPreferences,
  VideoItem,
} from '../types/nebula';
import { formatTime } from '../data/videos';
import { parseSubtitleFile } from '../utils/subtitleParser';

interface ImmersivePlayerProps {
  video: VideoItem;
  initialPosition?: number;
  preferences: UserPreferences;
  playerStyle: PlayerControlStyle;
  animationLevel: AnimationLevel;
  isFavorite: boolean;
  hasPrevious: boolean;
  hasNext: boolean;
  onPrevious: () => void;
  onNext: () => void;
  onToggleFavorite: (videoId: string) => void;
  onProgress: (videoId: string, position: number, duration: number) => void;
  onUpdatePreferences: (patch: Partial<UserPreferences>) => void;
  onClose: () => void;
}

const SPEED_OPTIONS = [0.5, 0.75, 1, 1.25, 1.5, 2];

export const ImmersivePlayer: React.FC<ImmersivePlayerProps> = ({
  video,
  initialPosition = 0,
  preferences,
  playerStyle,
  animationLevel,
  isFavorite,
  hasPrevious,
  hasNext,
  onPrevious,
  onNext,
  onToggleFavorite,
  onProgress,
  onUpdatePreferences,
  onClose,
}) => {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const [isPlaying, setIsPlaying] = useState(true);
  const [currentTime, setCurrentTime] = useState(() =>
    initialPosition > 0 && initialPosition < video.duration - 3 ? initialPosition : 0
  );
  const [duration, setDuration] = useState(video.duration);
  const [bufferedRatio, setBufferedRatio] = useState(0);
  const [playbackSpeed, setPlaybackSpeed] = useState(preferences.defaultSpeed);
  const [temp2xActive, setTemp2xActive] = useState(false);
  const speedBefore2xRef = useRef(preferences.defaultSpeed);

  const [volume, setVolume] = useState(0.85);
  const [isMuted, setIsMuted] = useState(false);
  const [brightness, setBrightness] = useState(1);
  const [subtitlesOn, setSubtitlesOn] = useState(preferences.subtitlesEnabled);
  const [customSubtitles, setCustomSubtitles] = useState<SubtitleCue[] | null>(null);
  const [customSubLabel, setCustomSubLabel] = useState<string | null>(null);

  const [isLocked, setIsLocked] = useState(false);
  const [controlsVisible, setControlsVisible] = useState(true);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showSpeedMenu, setShowSpeedMenu] = useState(false);
  const [showSubMenu, setShowSubMenu] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [fallbackMode, setFallbackMode] = useState(false);
  const [seekPreviewTime, setSeekPreviewTime] = useState<number | null>(null);
  const [seekPreviewPercent, setSeekPreviewPercent] = useState<number>(0);

  const [gestureHud, setGestureHud] = useState<{
    type: 'brightness' | 'volume' | 'seek';
    value: number;
    seekTarget?: number;
  } | null>(null);
  const [skipFeedback, setSkipFeedback] = useState<'back' | 'forward' | null>(null);

  const hideTimerRef = useRef<number | null>(null);
  const longPressTimerRef = useRef<number | null>(null);
  const gestureStateRef = useRef<{
    active: boolean;
    side: 'left' | 'right';
    startX: number;
    startY: number;
    startValue: number;
    startTime: number;
    axis: 'none' | 'vertical' | 'horizontal';
  } | null>(null);
  const lastTapRef = useRef<{ time: number; side: 'left' | 'right' } | null>(null);

  const resetHideTimer = useCallback(() => {
    if (hideTimerRef.current) window.clearTimeout(hideTimerRef.current);
    setControlsVisible(true);
    if (isPlaying && !showSubMenu && !showSpeedMenu) {
      hideTimerRef.current = window.setTimeout(() => {
        setControlsVisible(false);
        setShowSpeedMenu(false);
        setShowSubMenu(false);
      }, 3400);
    }
  }, [isPlaying, showSpeedMenu, showSubMenu]);

  useEffect(() => {
    resetHideTimer();
    return () => {
      if (hideTimerRef.current) window.clearTimeout(hideTimerRef.current);
      if (longPressTimerRef.current) window.clearTimeout(longPressTimerRef.current);
    };
  }, [resetHideTimer]);

  useEffect(() => {
    const el = videoRef.current;
    if (!el || fallbackMode) return;
    el.playbackRate = temp2xActive ? 2.0 : playbackSpeed;
    el.volume = isMuted ? 0 : volume;
  }, [playbackSpeed, temp2xActive, volume, isMuted, fallbackMode]);

  useEffect(() => {
    setCustomSubtitles(null);
    setCustomSubLabel(null);
    const el = videoRef.current;
    if (!el) return;

    setFallbackMode(false);
    setIsLoading(true);
    const resumeAt =
      initialPosition > 0 && initialPosition < video.duration - 3 ? initialPosition : 0;
    setCurrentTime(resumeAt);

    const handleLoadedMetadata = () => {
      if (Number.isFinite(el.duration) && el.duration > 0) {
        setDuration(el.duration);
      }
      if (resumeAt > 0 && resumeAt < (el.duration || video.duration)) {
        el.currentTime = resumeAt;
      }
      el.playbackRate = playbackSpeed;
      el.volume = isMuted ? 0 : volume;
      setIsLoading(false);
      el.play().catch(() => setIsPlaying(false));
    };

    const handleTimeUpdate = () => {
      setCurrentTime(el.currentTime);
      if (el.buffered.length > 0 && el.duration > 0) {
        setBufferedRatio(el.buffered.end(el.buffered.length - 1) / el.duration);
      }
      onProgress(video.id, el.currentTime, el.duration || duration);
    };

    const handleWaiting = () => setIsLoading(true);
    const handleCanPlay = () => setIsLoading(false);
    const handleEnded = () => {
      setIsPlaying(false);
      setControlsVisible(true);
    };
    const handleError = () => {
      setFallbackMode(true);
      setIsLoading(false);
      setIsPlaying(true);
    };

    el.addEventListener('loadedmetadata', handleLoadedMetadata);
    el.addEventListener('timeupdate', handleTimeUpdate);
    el.addEventListener('waiting', handleWaiting);
    el.addEventListener('canplay', handleCanPlay);
    el.addEventListener('ended', handleEnded);
    el.addEventListener('error', handleError);

    return () => {
      el.removeEventListener('loadedmetadata', handleLoadedMetadata);
      el.removeEventListener('timeupdate', handleTimeUpdate);
      el.removeEventListener('waiting', handleWaiting);
      el.removeEventListener('canplay', handleCanPlay);
      el.removeEventListener('ended', handleEnded);
      el.removeEventListener('error', handleError);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [video.id, video.videoUrl, initialPosition]);

  useEffect(() => {
    if (!fallbackMode) return;
    let rafId = 0;
    let lastTick = performance.now();
    let lastSaved = 0;

    const paletteMap: Record<VideoItem['visualTheme'], [string, string, string]> = {
      cosmos: ['#090218', '#9333ea', '#3b82f6'],
      city: ['#040914', '#2563eb', '#38bdf8'],
      aurora: ['#021312', '#10b981', '#6366f1'],
      drive: ['#0b0914', '#f59e0b', '#3b82f6'],
      ocean: ['#020817', '#06b6d4', '#6366f1'],
    };
    const [bg, c1, c2] = paletteMap[video.visualTheme];

    const renderFrame = (now: number) => {
      const dt = Math.min(0.1, (now - lastTick) / 1000);
      lastTick = now;
      const effectiveSpeed = temp2xActive ? 2.0 : playbackSpeed;

      if (isPlaying) {
        setCurrentTime((prev) => {
          const next = prev + dt * effectiveSpeed;
          if (next >= duration) {
            setIsPlaying(false);
            return duration;
          }
          if (now - lastSaved > 1000) {
            lastSaved = now;
            onProgress(video.id, next, duration);
          }
          return next;
        });
      }

      const canvas = canvasRef.current;
      if (canvas) {
        const ctx = canvas.getContext('2d');
        if (ctx) {
          const w = canvas.width;
          const h = canvas.height;
          const t = now * 0.001 * effectiveSpeed;

          ctx.fillStyle = bg;
          ctx.fillRect(0, 0, w, h);

          for (let i = 0; i < 3; i++) {
            const x = w * (0.3 + 0.4 * Math.sin(t * 0.35 + i * 2.1));
            const y = h * (0.35 + 0.3 * Math.cos(t * 0.28 + i * 1.7));
            const r = Math.max(w, h) * (0.35 + 0.08 * Math.sin(t + i));
            const grad = ctx.createRadialGradient(x, y, 0, x, y, r);
            grad.addColorStop(0, i % 2 === 0 ? `${c1}55` : `${c2}44`);
            grad.addColorStop(1, 'rgba(0,0,0,0)');
            ctx.fillStyle = grad;
            ctx.fillRect(0, 0, w, h);
          }
        }
      }
      rafId = requestAnimationFrame(renderFrame);
    };

    rafId = requestAnimationFrame(renderFrame);
    return () => cancelAnimationFrame(rafId);
  }, [fallbackMode, isPlaying, playbackSpeed, temp2xActive, duration, onProgress, video.id, video.visualTheme]);

  const togglePlay = useCallback(() => {
    if (isLocked) return;
    if (fallbackMode) {
      setIsPlaying((prev) => !prev);
      resetHideTimer();
      return;
    }
    const el = videoRef.current;
    if (!el) return;
    if (el.paused) {
      el.play().then(() => setIsPlaying(true)).catch(() => {});
    } else {
      el.pause();
      setIsPlaying(false);
    }
    resetHideTimer();
  }, [fallbackMode, isLocked, resetHideTimer]);

  const seekTo = useCallback(
    (targetTime: number) => {
      if (isLocked) return;
      const clamped = Math.max(0, Math.min(duration, targetTime));
      setCurrentTime(clamped);
      if (!fallbackMode && videoRef.current) {
        videoRef.current.currentTime = clamped;
      }
      onProgress(video.id, clamped, duration);
      resetHideTimer();
    },
    [duration, fallbackMode, isLocked, onProgress, resetHideTimer, video.id]
  );

  const skipBy = useCallback(
    (delta: number) => {
      if (isLocked) return;
      seekTo(currentTime + delta);
      setSkipFeedback(delta < 0 ? 'back' : 'forward');
      window.setTimeout(() => setSkipFeedback(null), 550);
    },
    [currentTime, isLocked, seekTo]
  );

  const toggleFullscreen = useCallback(() => {
    const el = containerRef.current;
    if (!el) return;
    if (!document.fullscreenElement) {
      el.requestFullscreen?.().catch(() => {});
    } else {
      document.exitFullscreen?.().catch(() => {});
    }
  }, []);

  const togglePictureInPicture = useCallback(() => {
    const el = videoRef.current;
    if (!el || fallbackMode) return;
    if (document.pictureInPictureElement) {
      document.exitPictureInPicture?.().catch(() => {});
    } else if (document.pictureInPictureEnabled) {
      el.requestPictureInPicture?.().catch(() => {});
    }
  }, [fallbackMode]);

  useEffect(() => {
    const onFsChange = () => setIsFullscreen(Boolean(document.fullscreenElement));
    document.addEventListener('fullscreenchange', onFsChange);
    return () => document.removeEventListener('fullscreenchange', onFsChange);
  }, []);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !document.fullscreenElement) {
        onClose();
        return;
      }
      if (isLocked && e.key.toLowerCase() !== 'l') return;

      switch (e.key.toLowerCase()) {
        case ' ':
        case 'k':
          e.preventDefault();
          togglePlay();
          break;
        case 'arrowleft':
          e.preventDefault();
          skipBy(-10);
          break;
        case 'arrowright':
          e.preventDefault();
          skipBy(10);
          break;
        case 'arrowup':
          e.preventDefault();
          setIsMuted(false);
          setVolume((v) => Math.min(1, +(v + 0.1).toFixed(2)));
          break;
        case 'arrowdown':
          e.preventDefault();
          setVolume((v) => Math.max(0, +(v - 0.1).toFixed(2)));
          break;
        case 'm':
          setIsMuted((m) => !m);
          break;
        case 'c':
          setSubtitlesOn((s) => !s);
          break;
        case 'f':
          toggleFullscreen();
          break;
        case 'l':
          setIsLocked((l) => !l);
          break;
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [isLocked, onClose, skipBy, toggleFullscreen, togglePlay]);

  const handleSurfacePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (isLocked) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const relX = (e.clientX - rect.left) / rect.width;
    const side: 'left' | 'right' = relX < 0.5 ? 'left' : 'right';

    gestureStateRef.current = {
      active: true,
      side,
      startX: e.clientX,
      startY: e.clientY,
      startValue: side === 'left' ? brightness : isMuted ? 0 : volume,
      startTime: currentTime,
      axis: 'none',
    };

    if (preferences.longPress2xEnabled) {
      if (longPressTimerRef.current) window.clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = window.setTimeout(() => {
        if (gestureStateRef.current?.axis === 'none') {
          speedBefore2xRef.current = playbackSpeed;
          setTemp2xActive(true);
        }
      }, 420);
    }
  };

  const handleSurfacePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    const g = gestureStateRef.current;
    if (!g || !g.active || isLocked) return;

    const dx = e.clientX - g.startX;
    const dy = g.startY - e.clientY;

    if (g.axis === 'none' && (Math.abs(dx) > 14 || Math.abs(dy) > 14)) {
      if (longPressTimerRef.current) window.clearTimeout(longPressTimerRef.current);
      g.axis = Math.abs(dx) > Math.abs(dy) * 1.25 ? 'horizontal' : 'vertical';
    }

    if (g.axis === 'vertical') {
      const sensitivity = 220;
      if (g.side === 'left' && preferences.brightnessSwipeEnabled) {
        const next = Math.max(0.3, Math.min(1.5, g.startValue + dy / sensitivity));
        setBrightness(next);
        setGestureHud({ type: 'brightness', value: Math.round(((next - 0.3) / 1.2) * 100) });
      } else if (g.side === 'right' && preferences.volumeSwipeEnabled) {
        const next = Math.max(0, Math.min(1, g.startValue + dy / sensitivity));
        setIsMuted(next === 0);
        setVolume(next);
        setGestureHud({ type: 'volume', value: Math.round(next * 100) });
      }
    } else if (g.axis === 'horizontal' && preferences.horizontalSeekEnabled && duration > 0) {
      const target = Math.max(0, Math.min(duration, g.startTime + (dx / 450) * 60));
      setGestureHud({
        type: 'seek',
        value: Math.round((target / duration) * 100),
        seekTarget: target,
      });
    }
  };

  const handleSurfacePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    if (longPressTimerRef.current) window.clearTimeout(longPressTimerRef.current);
    const was2x = temp2xActive;
    if (was2x) {
      setTemp2xActive(false);
    }

    const g = gestureStateRef.current;
    gestureStateRef.current = null;

    if (g?.axis === 'horizontal' && gestureHud?.type === 'seek' && gestureHud.seekTarget !== undefined) {
      seekTo(gestureHud.seekTarget);
      window.setTimeout(() => setGestureHud(null), 350);
      return;
    }

    window.setTimeout(() => setGestureHud(null), 650);
    if (g?.axis !== 'none' || was2x) return;

    if (isLocked) {
      resetHideTimer();
      return;
    }

    const rect = e.currentTarget.getBoundingClientRect();
    const relX = (e.clientX - rect.left) / rect.width;
    const side: 'left' | 'right' = relX < 0.5 ? 'left' : 'right';
    const now = Date.now();

    if (
      preferences.doubleTapSeekEnabled &&
      lastTapRef.current &&
      now - lastTapRef.current.time < 280 &&
      lastTapRef.current.side === side
    ) {
      lastTapRef.current = null;
      skipBy(side === 'left' ? -10 : 10);
    } else {
      lastTapRef.current = { time: now, side };
      setControlsVisible((v) => !v);
      if (!controlsVisible) resetHideTimer();
    }
  };

  const handleLoadExternalSubtitle = async (file: File) => {
    const text = await file.text();
    const cues = parseSubtitleFile(text, file.name);
    setCustomSubtitles(cues);
    setCustomSubLabel(file.name);
    setSubtitlesOn(true);
  };

  const activeSubtitle = useMemo(() => {
    if (!subtitlesOn) return null;
    const cues = customSubtitles ?? video.subtitles;
    if (cues.length === 0) return null;
    const adjustedTime = currentTime - preferences.subtitleDelayMs / 1000;
    const loopedTime = duration > 0 ? Math.max(0, adjustedTime) % Math.max(35, duration) : adjustedTime;
    return (
      cues.find((cue) => adjustedTime >= cue.start && adjustedTime <= cue.end) ??
      (!customSubtitles ? cues.find((cue) => loopedTime >= cue.start && loopedTime <= cue.end) : undefined) ??
      null
    );
  }, [currentTime, customSubtitles, duration, preferences.subtitleDelayMs, subtitlesOn, video.subtitles]);

  const progressPercent = duration > 0 ? Math.min(100, (currentTime / duration) * 100) : 0;

  return (
    <motion.div
      ref={containerRef}
      initial={animationLevel === 'none' ? false : { opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={animationLevel === 'none' ? undefined : { opacity: 0 }}
      transition={{ duration: 0.22 }}
      className="fixed inset-0 z-50 bg-black text-white flex flex-col select-none overflow-hidden"
    >
      {/* Video / Procedural Canvas Viewport */}
      <div
        className="relative flex-1 flex items-center justify-center overflow-hidden touch-none"
        style={{ filter: `brightness(${brightness})` }}
        onPointerDown={handleSurfacePointerDown}
        onPointerMove={handleSurfacePointerMove}
        onPointerUp={handleSurfacePointerUp}
      >
        {!fallbackMode ? (
          <video
            ref={videoRef}
            src={video.videoUrl}
            poster={video.thumbnail}
            playsInline
            className="w-full h-full object-contain bg-black pointer-events-none"
          />
        ) : (
          <canvas
            ref={canvasRef}
            width={1280}
            height={720}
            className="w-full h-full object-cover pointer-events-none"
          />
        )}

        {isLoading && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/35 pointer-events-none">
            <div className="w-12 h-12 rounded-full border-2 border-white/20 border-t-[var(--nebula-accent)] animate-spin" />
          </div>
        )}

        {/* Long-press 2.0x indicator */}
        <AnimatePresence>
          {temp2xActive && (
            <motion.div
              initial={{ opacity: 0, y: -6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              className="absolute top-20 left-1/2 -translate-x-1/2 px-4 py-2 rounded-xl bg-black/80 backdrop-blur-md border border-[var(--nebula-accent)] flex items-center gap-2 pointer-events-none"
            >
              <FastForward className="w-4 h-4 text-[var(--nebula-accent)]" />
              <span className="font-mono-tabular text-xs font-semibold">2.0x Speed</span>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Double-Tap Skip Ripple */}
        <AnimatePresence>
          {skipFeedback && (
            <motion.div
              initial={{ opacity: 0, scale: 0.85 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 1.05 }}
              className={`absolute top-1/2 -translate-y-1/2 px-5 py-3 rounded-2xl bg-black/70 backdrop-blur-md border border-white/15 flex items-center gap-2 pointer-events-none ${
                skipFeedback === 'back' ? 'left-8 sm:left-16' : 'right-8 sm:right-16'
              }`}
            >
              {skipFeedback === 'back' ? (
                <>
                  <Rewind className="w-5 h-5 text-[var(--nebula-accent)]" />
                  <span className="font-mono-tabular text-sm font-semibold">-10s</span>
                </>
              ) : (
                <>
                  <span className="font-mono-tabular text-sm font-semibold">+10s</span>
                  <FastForward className="w-5 h-5 text-[var(--nebula-accent)]" />
                </>
              )}
            </motion.div>
          )}
        </AnimatePresence>

        {/* Brightness / Volume / Horizontal Seek HUD */}
        <AnimatePresence>
          {gestureHud && (
            <motion.div
              initial={{ opacity: 0, scale: 0.92 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0 }}
              className={`absolute top-1/2 -translate-y-1/2 px-4 py-4 rounded-2xl bg-black/75 backdrop-blur-xl border border-white/15 flex flex-col items-center gap-2.5 pointer-events-none ${
                gestureHud.type === 'brightness'
                  ? 'left-5 sm:left-10'
                  : gestureHud.type === 'volume'
                  ? 'right-5 sm:right-10'
                  : 'left-1/2 -translate-x-1/2'
              }`}
            >
              {gestureHud.type === 'seek' ? (
                <div className="text-center px-2">
                  <p className="font-mono-tabular text-lg font-bold">
                    {formatTime(gestureHud.seekTarget ?? 0)}
                  </p>
                  <p className="text-[11px] text-slate-300">Release to seek</p>
                </div>
              ) : (
                <>
                  {gestureHud.type === 'brightness' ? (
                    <Sun className="w-5 h-5 text-amber-400" />
                  ) : gestureHud.value === 0 ? (
                    <VolumeX className="w-5 h-5 text-rose-400" />
                  ) : (
                    <Volume2 className="w-5 h-5 text-[var(--nebula-accent)]" />
                  )}
                  <div className="w-1.5 h-28 rounded-full bg-white/20 overflow-hidden flex flex-col justify-end">
                    <div
                      className="w-full nebula-accent-gradient rounded-full transition-all duration-75"
                      style={{ height: `${gestureHud.value}%` }}
                    />
                  </div>
                  <span className="font-mono-tabular text-xs font-medium">
                    {gestureHud.value}%
                  </span>
                </>
              )}
            </motion.div>
          )}
        </AnimatePresence>

        {/* Dynamic Styled Subtitles Overlay */}
        <AnimatePresence>
          {activeSubtitle && (
            <motion.div
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              className="absolute left-1/2 -translate-x-1/2 max-w-2xl px-4 pointer-events-none text-center transition-all"
              style={{
                bottom: controlsVisible && !isLocked
                  ? '156px'
                  : `${preferences.subtitlePositionBottom}%`,
              }}
            >
              <span
                className="inline-block px-4 py-2 rounded-xl backdrop-blur-md border border-white/10 font-medium leading-relaxed shadow-lg whitespace-pre-line"
                style={{
                  fontSize: `${preferences.subtitleFontSize}px`,
                  color: preferences.subtitleColor,
                  backgroundColor: `rgba(0, 0, 0, ${preferences.subtitleBgOpacity})`,
                }}
              >
                {activeSubtitle.text}
              </span>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Screen Lock Floating Unlock Button */}
      {isLocked && controlsVisible && (
        <div className="absolute bottom-8 left-1/2 -translate-x-1/2 z-30">
          <button
            type="button"
            onClick={() => {
              setIsLocked(false);
              resetHideTimer();
            }}
            className="min-h-[44px] px-5 py-2.5 rounded-2xl bg-black/75 backdrop-blur-xl border border-white/20 flex items-center gap-2 text-sm font-medium text-white shadow-2xl hover:border-[var(--nebula-accent)] transition-colors"
          >
            <Unlock className="w-4 h-4 text-[var(--nebula-accent)]" />
            <span>Unlock Screen</span>
          </button>
        </div>
      )}

      {/* Top Glass Header */}
      <AnimatePresence>
        {controlsVisible && !isLocked && (
          <motion.header
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
            transition={{ duration: 0.18 }}
            className="absolute top-0 left-0 right-0 z-20 p-3 sm:p-5 bg-gradient-to-b from-black/85 via-black/45 to-transparent flex items-center justify-between gap-4"
          >
            <div className="flex items-center gap-2 sm:gap-3 min-w-0">
              <button
                type="button"
                onClick={onClose}
                aria-label="Back to library"
                className="min-h-[44px] min-w-[44px] rounded-xl bg-white/10 backdrop-blur-md border border-white/15 flex items-center justify-center hover:bg-white/20 transition-colors"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <div className="min-w-0">
                <h2 className="font-display font-semibold text-base sm:text-lg truncate">
                  {video.title}
                </h2>
                <p className="text-xs text-slate-300 truncate">
                  {video.category} · {video.resolution}
                  {customSubLabel ? ` · Sub: ${customSubLabel}` : ''}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              {!fallbackMode && (
                <button
                  type="button"
                  onClick={togglePictureInPicture}
                  aria-label="Picture in Picture"
                  className="min-h-[44px] min-w-[44px] rounded-xl bg-white/10 backdrop-blur-md border border-white/15 flex items-center justify-center hover:bg-white/20 transition-colors"
                >
                  <PictureInPicture2 className="w-5 h-5" />
                </button>
              )}
              <button
                type="button"
                onClick={() => onToggleFavorite(video.id)}
                aria-label={isFavorite ? 'Remove favorite' : 'Add favorite'}
                className="min-h-[44px] min-w-[44px] rounded-xl bg-white/10 backdrop-blur-md border border-white/15 flex items-center justify-center hover:bg-white/20 transition-colors"
              >
                <Heart
                  className={`w-5 h-5 ${
                    isFavorite ? 'fill-[var(--nebula-accent)] text-[var(--nebula-accent)]' : ''
                  }`}
                />
              </button>
              <button
                type="button"
                onClick={() => {
                  setIsLocked(true);
                  setControlsVisible(true);
                }}
                aria-label="Lock screen"
                className="min-h-[44px] min-w-[44px] rounded-xl bg-white/10 backdrop-blur-md border border-white/15 flex items-center justify-center hover:bg-white/20 transition-colors"
              >
                <Lock className="w-5 h-5" />
              </button>
            </div>
          </motion.header>
        )}
      </AnimatePresence>

      {/* Orbital Center Transport */}
      <AnimatePresence>
        {controlsVisible && !isLocked && playerStyle === 'orbital' && (
          <motion.div
            initial={{ opacity: 0, scale: 0.94 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.94 }}
            className="absolute inset-0 z-10 flex items-center justify-center gap-8 pointer-events-none"
          >
            <button
              type="button"
              onClick={() => skipBy(-10)}
              aria-label="Rewind 10 seconds"
              className="pointer-events-auto w-14 h-14 rounded-full bg-black/55 backdrop-blur-xl border border-white/20 flex items-center justify-center hover:scale-105 active:scale-95 transition-transform"
            >
              <Rewind className="w-6 h-6" />
            </button>

            <button
              type="button"
              onClick={togglePlay}
              aria-label={isPlaying ? 'Pause' : 'Play'}
              className="pointer-events-auto w-20 h-20 rounded-full nebula-accent-gradient nebula-accent-glow flex items-center justify-center shadow-2xl hover:scale-105 active:scale-95 transition-transform"
            >
              {isPlaying ? (
                <Pause className="w-8 h-8 fill-current" />
              ) : (
                <Play className="w-8 h-8 fill-current ml-1" />
              )}
            </button>

            <button
              type="button"
              onClick={() => skipBy(10)}
              aria-label="Forward 10 seconds"
              className="pointer-events-auto w-14 h-14 rounded-full bg-black/55 backdrop-blur-xl border border-white/20 flex items-center justify-center hover:scale-105 active:scale-95 transition-transform"
            >
              <FastForward className="w-6 h-6" />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Bottom Glass Control Panel */}
      <AnimatePresence>
        {controlsVisible && !isLocked && (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 16 }}
            transition={{ duration: 0.18 }}
            className="absolute bottom-0 left-0 right-0 z-20 p-3 sm:p-6 bg-gradient-to-t from-black/90 via-black/55 to-transparent"
          >
            <div
              className={`max-w-4xl mx-auto ${
                playerStyle === 'minimal'
                  ? 'px-2 py-2'
                  : 'rounded-2xl bg-slate-950/70 backdrop-blur-xl border border-white/15 p-3.5 sm:p-4 shadow-2xl'
              }`}
            >
              {/* Seekbar */}
              <div className="relative group/seek py-2">
                {seekPreviewTime !== null && (
                  <div
                    className="absolute -top-8 -translate-x-1/2 px-2.5 py-1 rounded-lg bg-black/90 border border-white/15 font-mono-tabular text-xs font-medium pointer-events-none"
                    style={{ left: `${seekPreviewPercent}%` }}
                  >
                    {formatTime(seekPreviewTime)}
                  </div>
                )}

                <div
                  className="relative h-2 rounded-full bg-white/20 overflow-hidden cursor-pointer"
                  onPointerMove={(e) => {
                    const rect = e.currentTarget.getBoundingClientRect();
                    const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
                    setSeekPreviewPercent(pct * 100);
                    setSeekPreviewTime(pct * duration);
                  }}
                  onPointerLeave={() => setSeekPreviewTime(null)}
                  onClick={(e) => {
                    const rect = e.currentTarget.getBoundingClientRect();
                    const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
                    seekTo(pct * duration);
                  }}
                >
                  <div
                    className="absolute inset-y-0 left-0 bg-white/25 rounded-full"
                    style={{ width: `${Math.min(100, bufferedRatio * 100)}%` }}
                  />
                  <div
                    className="absolute inset-y-0 left-0 nebula-accent-gradient rounded-full transition-[width] duration-75"
                    style={{ width: `${progressPercent}%` }}
                  />
                </div>

                <div
                  className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 w-4 h-4 rounded-full bg-white shadow-md pointer-events-none transition-transform group-hover/seek:scale-125"
                  style={{ left: `${progressPercent}%` }}
                />
              </div>

              <div className="flex items-center justify-between text-xs font-mono-tabular text-slate-300 mt-1 mb-3">
                <span>{formatTime(currentTime)}</span>
                <span className="hidden sm:inline font-sans text-[11px] text-slate-400">
                  Swipe left brightness · Swipe right volume · Drag seek · Hold 2x
                </span>
                <span>{formatTime(duration)}</span>
              </div>

              {/* Transport & Feature Row */}
              <div className="flex items-center justify-between gap-2 flex-wrap sm:flex-nowrap">
                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    disabled={!hasPrevious}
                    onClick={onPrevious}
                    aria-label="Previous video"
                    className="min-h-[44px] min-w-[40px] rounded-xl bg-white/10 hover:bg-white/20 disabled:opacity-35 flex items-center justify-center transition-colors"
                  >
                    <SkipBack className="w-4 h-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => skipBy(-10)}
                    aria-label="Rewind 10 seconds"
                    className="min-h-[44px] min-w-[40px] rounded-xl bg-white/10 hover:bg-white/20 flex items-center justify-center transition-colors"
                  >
                    <Rewind className="w-4 h-4" />
                  </button>

                  <button
                    type="button"
                    onClick={togglePlay}
                    aria-label={isPlaying ? 'Pause' : 'Play'}
                    className="min-h-[46px] min-w-[46px] px-4 rounded-xl nebula-accent-gradient nebula-accent-glow text-white flex items-center justify-center shadow-lg active:scale-95 transition-transform"
                  >
                    {isPlaying ? (
                      <Pause className="w-5 h-5 fill-current" />
                    ) : (
                      <Play className="w-5 h-5 fill-current ml-0.5" />
                    )}
                  </button>

                  <button
                    type="button"
                    onClick={() => skipBy(10)}
                    aria-label="Forward 10 seconds"
                    className="min-h-[44px] min-w-[40px] rounded-xl bg-white/10 hover:bg-white/20 flex items-center justify-center transition-colors"
                  >
                    <FastForward className="w-4 h-4" />
                  </button>
                  <button
                    type="button"
                    disabled={!hasNext}
                    onClick={onNext}
                    aria-label="Next video"
                    className="min-h-[44px] min-w-[40px] rounded-xl bg-white/10 hover:bg-white/20 disabled:opacity-35 flex items-center justify-center transition-colors"
                  >
                    <SkipForward className="w-4 h-4" />
                  </button>
                </div>

                <div className="flex items-center gap-1.5 sm:gap-2">
                  <div className="hidden md:flex items-center gap-2 px-2.5 py-1.5 rounded-xl bg-white/10">
                    <button
                      type="button"
                      onClick={() => setIsMuted((m) => !m)}
                      aria-label={isMuted ? 'Unmute' : 'Mute'}
                      className="p-1 hover:text-[var(--nebula-accent)] transition-colors"
                    >
                      {isMuted || volume === 0 ? (
                        <VolumeX className="w-4 h-4" />
                      ) : (
                        <Volume2 className="w-4 h-4" />
                      )}
                    </button>
                    <input
                      type="range"
                      min={0}
                      max={1}
                      step={0.05}
                      value={isMuted ? 0 : volume}
                      onChange={(e) => {
                        const val = parseFloat(e.target.value);
                        setVolume(val);
                        setIsMuted(val === 0);
                      }}
                      aria-label="Volume"
                      className="w-16 accent-[var(--nebula-accent)] cursor-pointer"
                    />
                  </div>

                  {/* Speed Selector */}
                  <div className="relative">
                    <button
                      type="button"
                      onClick={() => {
                        setShowSpeedMenu((s) => !s);
                        setShowSubMenu(false);
                      }}
                      className="min-h-[44px] px-3 rounded-xl bg-white/10 hover:bg-white/20 flex items-center gap-1.5 text-xs font-mono-tabular font-semibold transition-colors whitespace-nowrap"
                    >
                      <Gauge className="w-4 h-4 text-[var(--nebula-accent)]" />
                      <span>{playbackSpeed}x</span>
                    </button>

                    {showSpeedMenu && (
                      <div className="absolute bottom-14 right-0 p-1.5 rounded-xl bg-slate-950/95 backdrop-blur-xl border border-white/15 shadow-2xl flex flex-col gap-0.5 min-w-[92px]">
                        {SPEED_OPTIONS.map((spd) => (
                          <button
                            key={spd}
                            type="button"
                            onClick={() => {
                              setPlaybackSpeed(spd);
                              setShowSpeedMenu(false);
                            }}
                            className={`px-3 py-2 rounded-lg text-xs font-mono-tabular text-left transition-colors ${
                              playbackSpeed === spd
                                ? 'nebula-accent-gradient text-white font-semibold'
                                : 'text-slate-300 hover:bg-white/10'
                            }`}
                          >
                            {spd}x
                          </button>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Subtitle Menu & Toggle */}
                  <div className="relative">
                    <button
                      type="button"
                      onClick={() => {
                        setShowSubMenu((s) => !s);
                        setShowSpeedMenu(false);
                      }}
                      aria-label="Subtitle options"
                      className={`min-h-[44px] min-w-[44px] rounded-xl flex items-center justify-center transition-colors ${
                        subtitlesOn
                          ? 'bg-[rgba(var(--nebula-accent-rgb),0.3)] border border-[rgba(var(--nebula-accent-rgb),0.7)] text-white'
                          : 'bg-white/10 hover:bg-white/20 text-slate-300'
                      }`}
                    >
                      <Captions className="w-5 h-5" />
                    </button>

                    {showSubMenu && (
                      <div className="absolute bottom-14 right-0 w-64 p-3.5 rounded-2xl bg-slate-950/95 backdrop-blur-xl border border-white/15 shadow-2xl space-y-3 text-xs">
                        <div className="flex items-center justify-between">
                          <span className="font-semibold">Subtitles</span>
                          <button
                            type="button"
                            onClick={() => setSubtitlesOn((s) => !s)}
                            className="px-2.5 py-1 rounded-lg bg-white/10 font-mono-tabular text-[var(--nebula-accent)]"
                          >
                            {subtitlesOn ? 'ON' : 'OFF'}
                          </button>
                        </div>

                        <label className="w-full py-2 px-3 rounded-xl border border-dashed border-white/20 hover:border-[var(--nebula-accent)] flex items-center justify-center gap-2 cursor-pointer">
                          <FileText className="w-4 h-4 text-[var(--nebula-accent)]" />
                          <span>Load .SRT / .VTT / .ASS</span>
                          <input
                            type="file"
                            accept=".srt,.vtt,.ass,.ssa,.sub,text/plain"
                            className="sr-only"
                            onChange={(e) => {
                              const file = e.target.files?.[0];
                              if (file) handleLoadExternalSubtitle(file);
                              e.target.value = '';
                            }}
                          />
                        </label>

                        <div className="space-y-1">
                          <div className="flex justify-between text-slate-300">
                            <span>Delay</span>
                            <span className="font-mono-tabular">{preferences.subtitleDelayMs} ms</span>
                          </div>
                          <div className="grid grid-cols-3 gap-1">
                            <button
                              type="button"
                              onClick={() =>
                                onUpdatePreferences({ subtitleDelayMs: preferences.subtitleDelayMs - 250 })
                              }
                              className="py-1.5 rounded-lg bg-white/10 hover:bg-white/20"
                            >
                              -250ms
                            </button>
                            <button
                              type="button"
                              onClick={() => onUpdatePreferences({ subtitleDelayMs: 0 })}
                              className="py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-[var(--nebula-accent)]"
                            >
                              0ms
                            </button>
                            <button
                              type="button"
                              onClick={() =>
                                onUpdatePreferences({ subtitleDelayMs: preferences.subtitleDelayMs + 250 })
                              }
                              className="py-1.5 rounded-lg bg-white/10 hover:bg-white/20"
                            >
                              +250ms
                            </button>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Fullscreen Toggle */}
                  <button
                    type="button"
                    onClick={toggleFullscreen}
                    aria-label={isFullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}
                    className="min-h-[44px] min-w-[44px] rounded-xl bg-white/10 hover:bg-white/20 flex items-center justify-center transition-colors"
                  >
                    {isFullscreen ? (
                      <Minimize className="w-5 h-5" />
                    ) : (
                      <Maximize className="w-5 h-5" />
                    )}
                  </button>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};
