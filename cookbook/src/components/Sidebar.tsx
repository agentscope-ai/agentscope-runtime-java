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
















