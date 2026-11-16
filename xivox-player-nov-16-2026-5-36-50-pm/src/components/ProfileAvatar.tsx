import React, { useState } from 'react';
import { AVATAR_OPTIONS } from '../data/videos';

interface ProfileAvatarProps {
  profileName: string;
  avatarId: string;
  customAvatarUrl?: string;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export const ProfileAvatar: React.FC<ProfileAvatarProps> = ({
  profileName,
  avatarId,
  customAvatarUrl,
  size = 'md',
  className = '',
}) => {
  const [imgError, setImgError] = useState(false);

  const sizeClass =
    size === 'sm' ? 'w-9 h-9 text-xs' : size === 'lg' ? 'w-16 h-16 text-xl' : 'w-11 h-11 text-sm';

  const selected = AVATAR_OPTIONS.find((opt) => opt.id === avatarId);
  const src = avatarId === 'custom' && customAvatarUrl ? customAvatarUrl : selected?.imageUrl;

  const initials = (profileName || 'NP')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('');

  return (
    <div
      className={`relative rounded-full p-[2px] nebula-accent-gradient shrink-0 ${sizeClass} ${className}`}
    >
      <div className="w-full h-full rounded-full overflow-hidden bg-slate-950 flex items-center justify-center">
        {src && !imgError ? (
          <img
            src={src}
            alt={profileName}
            referrerPolicy="no-referrer"
            onError={() => setImgError(true)}
            className="w-full h-full object-cover"
          />
        ) : (
          <span className="font-display font-semibold tracking-tight text-white">
            {initials || 'NP'}
          </span>
        )}
      </div>
    </div>
  );
};
