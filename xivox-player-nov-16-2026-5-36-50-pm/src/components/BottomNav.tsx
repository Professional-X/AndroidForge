import React from 'react';
import { Film, Heart, Home, SlidersHorizontal } from 'lucide-react';
import { motion } from 'motion/react';
import { AnimationLevel, NavTab } from '../types/nebula';

interface BottomNavProps {
  activeTab: NavTab;
  onChangeTab: (tab: NavTab) => void;
  favoritesCount: number;
  animationLevel: AnimationLevel;
}

const NAV_ITEMS: { id: NavTab; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { id: 'home', label: 'Home', icon: Home },
  { id: 'library', label: 'Library', icon: Film },
  { id: 'favorites', label: 'Favorites', icon: Heart },
  { id: 'personalize', label: 'Studio', icon: SlidersHorizontal },
];

export const BottomNav: React.FC<BottomNavProps> = ({
  activeTab,
  onChangeTab,
  favoritesCount,
  animationLevel,
}) => {
  return (
    <nav
      aria-label="Primary navigation"
      className="fixed bottom-3 left-3 right-3 sm:left-1/2 sm:-translate-x-1/2 sm:w-full sm:max-w-lg z-40 nebula-glass rounded-2xl shadow-2xl px-2 py-1.5"
    >
      <div className="grid grid-cols-4 items-center gap-1">
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => onChangeTab(item.id)}
              aria-current={isActive ? 'page' : undefined}
              className={`relative min-h-[48px] flex flex-col items-center justify-center rounded-xl transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--nebula-accent)] ${
                isActive
                  ? 'text-[var(--nebula-text)]'
                  : 'text-[var(--nebula-text-muted)] hover:text-[var(--nebula-text)]'
              }`}
            >
              {isActive && (
                <motion.span
                  layoutId={animationLevel === 'none' ? undefined : 'nebula-nav-pill'}
                  transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
                  className="absolute inset-0 rounded-xl bg-[rgba(var(--nebula-accent-rgb),0.15)] border border-[rgba(var(--nebula-accent-rgb),0.35)]"
                />
              )}
              <span className="relative z-10 flex items-center justify-center">
                <Icon
                  className={`w-5 h-5 ${
                    isActive ? 'text-[var(--nebula-accent)]' : ''
                  }`}
                />
                {item.id === 'favorites' && favoritesCount > 0 && (
                  <span className="ml-1 text-[10px] font-mono-tabular font-semibold text-[var(--nebula-accent)]">
                    {favoritesCount}
                  </span>
                )}
              </span>
              <span className="relative z-10 text-[11px] font-medium tracking-tight mt-0.5 whitespace-nowrap">
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
