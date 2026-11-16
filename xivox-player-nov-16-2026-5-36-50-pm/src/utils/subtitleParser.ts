import { SubtitleCue } from '../types/nebula';

function parseTimestamp(token: string): number | null {
  const normalized = token.trim().replace(',', '.');
  const parts = normalized.split(':');
  if (parts.length === 3) {
    const h = Number(parts[0]);
    const m = Number(parts[1]);
    const s = Number(parts[2]);
    if ([h, m, s].some(Number.isNaN)) return null;
    return h * 3600 + m * 60 + s;
  }
  if (parts.length === 2) {
    const m = Number(parts[0]);
    const s = Number(parts[1]);
    if ([m, s].some(Number.isNaN)) return null;
    return m * 60 + s;
  }
  return null;
}

export function parseSubtitleFile(rawText: string, fileName: string): SubtitleCue[] {
  const clean = rawText.replace(/^\uFEFF/, '').replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  const lower = fileName.toLowerCase();

  if (lower.endsWith('.ass') || lower.endsWith('.ssa') || clean.includes('[Events]')) {
    const cues: SubtitleCue[] = [];
    for (const line of clean.split('\n')) {
      if (!line.startsWith('Dialogue:')) continue;
      const parts = line.slice(9).trim().split(',');
      if (parts.length < 10) continue;
      const start = parseTimestamp(parts[1]);
      const end = parseTimestamp(parts[2]);
      if (start === null || end === null) continue;
      const text = parts
        .slice(9)
        .join(',')
        .replace(/\{[^}]*\}/g, '')
        .replace(/\\N/g, '\n')
        .trim();
      if (text) cues.push({ start, end, text });
    }
    return cues.sort((a, b) => a.start - b.start);
  }

  const blocks = clean.split(/\n{2,}/);
  const cues: SubtitleCue[] = [];
  for (const block of blocks) {
    const lines = block.split('\n').map((l) => l.trimEnd()).filter(Boolean);
    const arrowIdx = lines.findIndex((l) => l.includes('-->'));
    if (arrowIdx === -1) continue;
    const [startRaw, endRawWithSettings] = lines[arrowIdx].split('-->');
    const start = parseTimestamp(startRaw);
    const end = parseTimestamp((endRawWithSettings || '').trim().split(/\s+/)[0]);
    if (start === null || end === null) continue;
    const text = lines
      .slice(arrowIdx + 1)
      .join('\n')
      .replace(/<[^>]+>/g, '')
      .trim();
    if (text) cues.push({ start, end, text });
  }
  return cues.sort((a, b) => a.start - b.start);
}
