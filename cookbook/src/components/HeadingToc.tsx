/*
 * Copyright 2025 Alibaba
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
















