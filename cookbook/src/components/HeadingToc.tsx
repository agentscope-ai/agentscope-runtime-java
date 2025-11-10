import clsx from 'clsx';
import type { Heading } from '@/utils/markdown';

interface HeadingTocProps {
  headings: Heading[];
  activeSlug?: string;
  onNavigate: (slug: string) => void;
}

export function HeadingToc({ headings, activeSlug, onNavigate }: HeadingTocProps) {
  return (
    <aside className="toc">
      <h2 className="toc__title">Table of Contents</h2>
      <nav className="toc__list" aria-label="Page heading navigation">
        {headings.map((heading) => (
          <button
            key={heading.slug}
            className={clsx('toc__item', `toc__item--depth-${heading.depth}`, {
              'is-active': heading.slug === activeSlug,
            })}
            onClick={() => onNavigate(heading.slug)}
          >
            {heading.text}
          </button>
        ))}
        {headings.length === 0 && (
          <div className="toc__empty">This document has no navigable headings</div>
        )}
      </nav>
    </aside>
  );
}

export default HeadingToc;
















