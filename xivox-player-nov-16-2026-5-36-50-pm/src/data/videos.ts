import { VideoItem } from '../types/nebula';

export const PILOT_AVATAR_URL = '/src/assets/images/nebula_avatar_pilot_1794805909101.jpg';

export interface AccentPreset {
  id: 'nebula' | 'cyan' | 'amber' | 'emerald' | 'crimson';
  label: string;
  primary: string;
  secondary: string;
}

export const ACCENT_PRESETS: AccentPreset[] = [
  {
    id: 'nebula',
    label: 'XIVOX Neon',
    primary: '#9B5CFF',
    secondary: '#00C8FF',
  },
  {
    id: 'cyan',
    label: 'Cyber Cyan',
    primary: '#06b6d4',
    secondary: '#3b82f6',
  },
  {
    id: 'amber',
    label: 'Solar Amber',
    primary: '#f59e0b',
    secondary: '#ef4444',
  },
  {
    id: 'emerald',
    label: 'Emerald Pulse',
    primary: '#10b981',
    secondary: '#06b6d4',
  },
  {
    id: 'crimson',
    label: 'Crimson Nova',
    primary: '#f43f5e',
    secondary: '#a855f7',
  },
];

export const AVATAR_OPTIONS = [
  {
    id: 'pilot',
    label: 'Obsidian Visor',
    imageUrl: PILOT_AVATAR_URL,
  },
  {
    id: 'cosmos',
    label: 'Nebula Prism',
    imageUrl: '/src/assets/images/nebula_thumb_cosmos_1794805906934.jpg',
  },
  {
    id: 'aurora',
    label: 'Aurora Crest',
    imageUrl: '/src/assets/images/nebula_thumb_aurora_1794805907854.jpg',
  },
  {
    id: 'monogram',
    label: 'Monogram Glyph',
    imageUrl: '',
  },
];

export const INITIAL_VIDEOS: VideoItem[] = [
  {
    id: 'vid-cosmos-drift',
    title: 'Orion Nebula: Deep Horizon',
    subtitle: 'Volumetric stellar nurseries captured in wide-field spectral pass',
    category: 'Cosmos',
    duration: 254,
    resolution: '4K HDR',
    sizeLabel: '482 MB',
    thumbnail: '/src/assets/images/nebula_thumb_cosmos_1794805906934.jpg',
    videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    visualTheme: 'cosmos',
    addedAt: Date.now() - 86400000 * 1,
    subtitles: [
      { start: 1, end: 6, text: 'Entering the outer ionized hydrogen cloud of Orion.' },
      { start: 7, end: 13, text: 'Violet emission bands trace newborn stars igniting within the dust.' },
      { start: 14, end: 22, text: 'Solar winds sculpt filaments stretching light-years across the void.' },
      { start: 23, end: 32, text: 'Long-exposure spectral sensors reveal structures invisible to the eye.' },
    ],
  },
  {
    id: 'vid-midnight-metropolis',
    title: 'Midnight Grid: Rain Reflections',
    subtitle: 'Anamorphic nocturnal study through glass towers and wet expressways',
    category: 'Urban',
    duration: 186,
    resolution: '4K 60fps',
    sizeLabel: '340 MB',
    thumbnail: '/src/assets/images/nebula_thumb_cybercity_1794805907818.jpg',
    videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
    visualTheme: 'city',
    addedAt: Date.now() - 86400000 * 2,
    subtitles: [
      { start: 1, end: 6, text: 'After midnight, the downtown corridor slows to a steady pulse.' },
      { start: 7, end: 14, text: 'Sodium and LED reflections stretch across rain-slicked asphalt.' },
      { start: 15, end: 24, text: 'Anamorphic optics turn distant tower windows into horizontal flares.' },
    ],
  },
  {
    id: 'vid-glacial-aurora',
    title: 'Subzero Aurora: Crystal Cavern',
    subtitle: 'Solar storm ribbons dancing above an Icelandic glacier vault',
    category: 'Nature',
    duration: 312,
    resolution: '4K HDR',
    sizeLabel: '615 MB',
    thumbnail: '/src/assets/images/nebula_thumb_aurora_1794805907854.jpg',
    videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',
    visualTheme: 'aurora',
    addedAt: Date.now() - 86400000 * 3,
    subtitles: [
      { start: 1, end: 7, text: 'Inside the meltwater vault, blue glacial ice acts like a prism.' },
      { start: 8, end: 15, text: 'Charged solar particles collide with the upper atmosphere at 65° North.' },
      { start: 16, end: 25, text: 'Emerald curtains ripple overhead in real-time silence.' },
    ],
  },
  {
    id: 'vid-coastal-tourer',
    title: 'Pacific Dusk: Silent Grand Tour',
    subtitle: 'Twilight coast run in an electric grand tourer along cliffside bends',
    category: 'Automotive',
    duration: 148,
    resolution: '1080p',
    sizeLabel: '218 MB',
    thumbnail: '/src/assets/images/nebula_thumb_synthwave_1794805908596.jpg',
    videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4',
    visualTheme: 'drive',
    addedAt: Date.now() - 86400000 * 5,
    subtitles: [
      { start: 1, end: 6, text: 'Coastal fog lifts off the ridge as dusk settles over Highway 1.' },
      { start: 7, end: 14, text: 'Only the whisper of tires on damp tarmac breaks the ocean wind.' },
      { start: 15, end: 22, text: 'Cooling twilight hues frame the last turn before the headland.' },
    ],
  },
  {
    id: 'vid-abyssal-drift',
    title: 'Abyssal Bioluminescence',
    subtitle: 'Deep-trench macro study of drifting translucent hydrozoans',
    category: 'Ocean',
    duration: 228,
    resolution: '4K HDR',
    sizeLabel: '410 MB',
    thumbnail: '/src/assets/images/nebula_thumb_ocean_1794805909545.jpg',
    videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4',
    visualTheme: 'ocean',
    addedAt: Date.now() - 86400000 * 6,
    subtitles: [
      { start: 1, end: 7, text: 'Eight hundred meters below the surface, sunlight disappears completely.' },
      { start: 8, end: 15, text: 'Bioluminescent rings pulse along the bell margin in electric blue.' },
      { start: 16, end: 24, text: 'Every contraction propels the organism effortlessly through zero-current dark.' },
    ],
  },
];

export function formatTime(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) return '00:00';
  const total = Math.floor(seconds);
  const hrs = Math.floor(total / 3600);
  const mins = Math.floor((total % 3600) / 60);
  const secs = total % 60;
  if (hrs > 0) {
    return `${hrs}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}
