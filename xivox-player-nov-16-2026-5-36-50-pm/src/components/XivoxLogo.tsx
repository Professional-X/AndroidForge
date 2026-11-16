import React from 'react';

export const XivoxLogo: React.FC<{ size?: number; className?: string }> = ({
  size = 34,
  className = '',
}) => {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 108 108"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="xivox-grad" x1="16" y1="16" x2="92" y2="92" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="var(--nebula-accent-secondary, #00C8FF)" />
          <stop offset="100%" stopColor="var(--nebula-accent, #9B5CFF)" />
        </linearGradient>
      </defs>
      <path
        d="M54 14L88.6 34V74L54 94L19.4 74V34L54 14Z"
        stroke="url(#xivox-grad)"
        strokeWidth="5.5"
        strokeLinejoin="round"
      />
      <path d="M40 34L70 54L40 74V34Z" fill="url(#xivox-grad)" />
      <path
        d="M32 76L76 32"
        stroke="currentColor"
        strokeWidth="5"
        strokeLinecap="round"
      />
    </svg>
  );
};
