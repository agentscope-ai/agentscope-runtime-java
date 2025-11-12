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
import { useMemo, useState } from 'react';
import clsx from 'clsx';

export interface SidebarItem {
  id: string;
  title: string;
  group?: string;
}

interface SidebarProps {
  items: SidebarItem[];
  activeId?: string;
  onSelect: (id: string) => void;
}

export function Sidebar({ items, activeId, onSelect }: SidebarProps) {
  const [query, setQuery] = useState('');

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) {
      return items;
    }
    return items.filter((item) => item.title.toLowerCase().includes(q));
  }, [items, query]);

  return (
    <aside className="sidebar">
      <div className="sidebar__header">
        <input
          className="sidebar__search"
          type="search"
          placeholder="Search documents..."
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
      </div>
      <nav className="sidebar__list" aria-label="Document list">
        {filtered.map((item) => (
          <button
            key={item.id}
            className={clsx('sidebar__item', { 'is-active': item.id === activeId })}
            onClick={() => onSelect(item.id)}
          >
            <span className="sidebar__item-title">{item.title}</span>
          </button>
        ))}
        {filtered.length === 0 && (
          <div className="sidebar__empty">No matching documents found</div>
        )}
      </nav>
    </aside>
  );
}

export default Sidebar;
















