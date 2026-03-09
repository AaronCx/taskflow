import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { AxiosError } from 'axios';
import { tasksApi } from '../api/tasks';
import { Task, TaskStatus } from '../types';
import { Layout } from '../components/Layout';
import { StatusBadge } from '../components/StatusBadge';
import { PriorityBadge } from '../components/PriorityBadge';
import { useAuth } from '../context/AuthContext';
import { format } from 'date-fns';

const STATUS_FILTERS: { label: string; value: TaskStatus | 'ALL' }[] = [
  { label: 'All',         value: 'ALL' },
  { label: 'To Do',       value: 'TODO' },
  { label: 'In Progress', value: 'IN_PROGRESS' },
  { label: 'Done',        value: 'DONE' },
];

type SortKey = 'newest' | 'oldest' | 'priority' | 'dueDate';

const SORT_OPTIONS: { label: string; value: SortKey }[] = [
  { label: 'Newest first',  value: 'newest' },
  { label: 'Oldest first',  value: 'oldest' },
  { label: 'Priority',      value: 'priority' },
  { label: 'Due date',      value: 'dueDate' },
];

const PRIORITY_ORDER: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 };

function sortTasks(tasks: Task[], key: SortKey): Task[] {
  const sorted = [...tasks];
  switch (key) {
    case 'newest':
      return sorted.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    case 'oldest':
      return sorted.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
    case 'priority':
      return sorted.sort((a, b) => (PRIORITY_ORDER[a.priority] ?? 9) - (PRIORITY_ORDER[b.priority] ?? 9));
    case 'dueDate':
      return sorted.sort((a, b) => {
        if (!a.dueDate && !b.dueDate) return 0;
        if (!a.dueDate) return 1;
        if (!b.dueDate) return -1;
        return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
      });
    default:
      return sorted;
  }
}

/**
 * Dashboard — main view showing the authenticated user's task list.
 * Supports status filtering, search, sorting, and delete operations.
 */
export function Dashboard() {
  const { user } = useAuth();

  const [tasks,       setTasks]       = useState<Task[]>([]);
  const [filter,      setFilter]      = useState<TaskStatus | 'ALL'>('ALL');
  const [search,      setSearch]      = useState('');
  const [sortBy,      setSortBy]      = useState<SortKey>('newest');
  const [loading,     setLoading]     = useState(true);
  const [error,       setError]       = useState<string | null>(null);
  const [deletingId,  setDeletingId]  = useState<number | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [bulkLoading, setBulkLoading] = useState(false);

  // Debounced search
  const [debouncedSearch, setDebouncedSearch] = useState('');
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 300);
    return () => clearTimeout(timer);
  }, [search]);

  // Fetch tasks whenever the filter or search changes
  const fetchTasks = useCallback(() => {
    setLoading(true);
    setError(null);

    const status = filter === 'ALL' ? undefined : filter;
    const q = debouncedSearch.trim() || undefined;

    tasksApi.getAll(status, q)
      .then((res) => setTasks(res.data))
      .catch((err: AxiosError) => {
        setError('Failed to load tasks. Please refresh.');
        console.error(err);
      })
      .finally(() => setLoading(false));
  }, [filter, debouncedSearch]);

  useEffect(() => { fetchTasks(); }, [fetchTasks]);

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this task?')) return;
    setDeletingId(id);
    try {
      await tasksApi.remove(id);
      setTasks((prev) => prev.filter((t) => t.id !== id));
    } catch {
      alert('Failed to delete task.');
    } finally {
      setDeletingId(null);
    }
  };

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === sortedTasks.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(tasks.map((t) => t.id)));
    }
  };

  const handleBulkStatus = async (status: TaskStatus) => {
    if (selectedIds.size === 0) return;
    setBulkLoading(true);
    try {
      await tasksApi.bulkUpdateStatus([...selectedIds], status);
      setSelectedIds(new Set());
      fetchTasks();
    } catch {
      alert('Bulk update failed.');
    } finally {
      setBulkLoading(false);
    }
  };

  const handleBulkDelete = async () => {
    if (selectedIds.size === 0) return;
    if (!confirm(`Delete ${selectedIds.size} task(s)?`)) return;
    setBulkLoading(true);
    try {
      await tasksApi.bulkDelete([...selectedIds]);
      setSelectedIds(new Set());
      fetchTasks();
    } catch {
      alert('Bulk delete failed.');
    } finally {
      setBulkLoading(false);
    }
  };

  // ── Stats cards (computed from ALL fetched tasks, before sort) ──
  const stats = {
    total:      tasks.length,
    todo:       tasks.filter((t) => t.status === 'TODO').length,
    inProgress: tasks.filter((t) => t.status === 'IN_PROGRESS').length,
    done:       tasks.filter((t) => t.status === 'DONE').length,
  };

  const sortedTasks = sortTasks(tasks, sortBy);

  return (
    <Layout>
      {/* ── Page header ────────────────────────────────────────────── */}
      <div className="mb-6 sm:mb-8">
        <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white">
          Good {getGreeting()}, {user?.firstName}
        </h1>
        <p className="mt-1 text-sm sm:text-base text-gray-500 dark:text-gray-400">Here's what's on your plate today.</p>
      </div>

      {/* ── Stats row ──────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4 mb-6 sm:mb-8">
        {[
          { label: 'Total',       value: stats.total,      color: 'text-gray-700',  bg: 'bg-gray-50'   },
          { label: 'To Do',       value: stats.todo,       color: 'text-gray-600',  bg: 'bg-gray-50'   },
          { label: 'In Progress', value: stats.inProgress, color: 'text-blue-700',  bg: 'bg-blue-50'   },
          { label: 'Done',        value: stats.done,       color: 'text-green-700', bg: 'bg-green-50'  },
        ].map(({ label, value, color, bg }) => (
          <div key={label} className={`${bg} rounded-xl p-3 sm:p-4 border border-gray-100`}>
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</p>
            <p className={`text-2xl sm:text-3xl font-bold mt-1 ${color}`}>{value}</p>
          </div>
        ))}
      </div>

      {/* ── Search + Filter + Sort row ────────────────────────────── */}
      <div className="flex flex-col sm:flex-row gap-3 mb-6">
        {/* Search */}
        <div className="relative flex-1">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none"
               viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search tasks..."
            className="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg text-sm
                       focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <div className="flex gap-2 sm:gap-3">
          {/* Status filter tabs */}
          <div className="flex gap-1 bg-gray-100 p-1 rounded-lg overflow-x-auto">
            {STATUS_FILTERS.map(({ label, value }) => (
              <button
                key={value}
                onClick={() => setFilter(value)}
                className={`px-3 sm:px-4 py-1.5 rounded-md text-sm font-medium transition-colors whitespace-nowrap ${
                  filter === value
                    ? 'bg-white text-gray-900 shadow-sm'
                    : 'text-gray-500 hover:text-gray-800'
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {/* Sort dropdown */}
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as SortKey)}
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white shrink-0
                       focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            {SORT_OPTIONS.map(({ label, value }) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </div>
      </div>

      {/* ── Bulk action bar ─────────────────────────────────────────── */}
      {sortedTasks.length > 0 && (
        <div className="flex items-center gap-3 mb-4">
          <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
            <input
              type="checkbox"
              checked={selectedIds.size === sortedTasks.length && sortedTasks.length > 0}
              onChange={toggleSelectAll}
              className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            Select all
          </label>
          {selectedIds.size > 0 && (
            <>
              <span className="text-xs text-gray-500">{selectedIds.size} selected</span>
              <div className="flex gap-1 ml-auto">
                <button
                  onClick={() => handleBulkStatus('TODO')}
                  disabled={bulkLoading}
                  className="px-3 py-1 text-xs font-medium rounded-lg border border-gray-300
                             hover:bg-gray-50 disabled:opacity-50"
                >
                  To Do
                </button>
                <button
                  onClick={() => handleBulkStatus('IN_PROGRESS')}
                  disabled={bulkLoading}
                  className="px-3 py-1 text-xs font-medium rounded-lg border border-blue-300
                             text-blue-700 hover:bg-blue-50 disabled:opacity-50"
                >
                  In Progress
                </button>
                <button
                  onClick={() => handleBulkStatus('DONE')}
                  disabled={bulkLoading}
                  className="px-3 py-1 text-xs font-medium rounded-lg border border-green-300
                             text-green-700 hover:bg-green-50 disabled:opacity-50"
                >
                  Done
                </button>
                <button
                  onClick={handleBulkDelete}
                  disabled={bulkLoading}
                  className="px-3 py-1 text-xs font-medium rounded-lg border border-red-300
                             text-red-700 hover:bg-red-50 disabled:opacity-50"
                >
                  Delete
                </button>
              </div>
            </>
          )}
        </div>
      )}

      {/* ── Task list ──────────────────────────────────────────────── */}
      {loading && (
        <div className="flex justify-center py-20">
          <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent
                          rounded-full animate-spin" />
        </div>
      )}

      {error && (
        <div className="p-4 bg-red-50 text-red-700 rounded-lg text-sm">{error}</div>
      )}

      {!loading && !error && tasks.length === 0 && (
        <div className="text-center py-20">
          <p className="mt-4 text-lg font-medium text-gray-600">
            {debouncedSearch ? 'No tasks match your search' : 'No tasks yet'}
          </p>
          <p className="text-sm text-gray-400 mt-1">
            {debouncedSearch ? 'Try a different search term' : 'Create your first task to get started'}
          </p>
          {!debouncedSearch && (
            <Link
              to="/tasks/new"
              className="mt-4 inline-block bg-blue-600 text-white px-6 py-2 rounded-lg
                         text-sm font-medium hover:bg-blue-700 transition-colors"
            >
              Create task
            </Link>
          )}
        </div>
      )}

      {!loading && !error && sortedTasks.length > 0 && (
        <div className="space-y-3">
          {sortedTasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onDelete={handleDelete}
              isDeleting={deletingId === task.id}
              selected={selectedIds.has(task.id)}
              onToggleSelect={() => toggleSelect(task.id)}
            />
          ))}
        </div>
      )}
    </Layout>
  );
}

// ── Task card sub-component ───────────────────────────────────────────────────

function TaskCard({
  task,
  onDelete,
  isDeleting,
  selected,
  onToggleSelect,
}: {
  task: Task;
  onDelete: (id: number) => void;
  isDeleting: boolean;
  selected: boolean;
  onToggleSelect: () => void;
}) {
  const isOverdue =
    task.dueDate && task.status !== 'DONE' && new Date(task.dueDate) < new Date();

  return (
    <div className={`bg-white dark:bg-gray-800 rounded-xl border p-4 sm:p-5 hover:shadow-sm transition-all group ${
      selected ? 'border-blue-400 bg-blue-50/30 dark:bg-blue-900/20' : 'border-gray-200 dark:border-gray-700 hover:border-blue-300'
    }`}>
      <div className="flex items-start gap-3 sm:gap-4">
        {/* Checkbox */}
        <input
          type="checkbox"
          checked={selected}
          onChange={onToggleSelect}
          className="mt-1 w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 shrink-0"
        />
        {/* Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-white">
              {task.title}
            </h3>
            <StatusBadge   status={task.status}   />
            <PriorityBadge priority={task.priority} />
            {task.categoryName && (
              <span
                className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium text-white"
                style={{ backgroundColor: task.categoryColor ?? '#6b7280' }}
              >
                {task.categoryName}
              </span>
            )}
          </div>

          {task.description && (
            <p className="mt-1 text-sm text-gray-500 line-clamp-2">{task.description}</p>
          )}

          <div className="mt-2 flex items-center justify-between gap-2">
            <div className="flex items-center gap-3 sm:gap-4 text-xs text-gray-400">
              {task.dueDate && (
                <span className={isOverdue ? 'text-red-500 font-medium' : ''}>
                  {isOverdue ? 'Overdue: ' : 'Due: '}
                  {format(new Date(task.dueDate), 'MMM d, yyyy')}
                </span>
              )}
              {task.assignedToName && (
                <span className="hidden sm:inline">Assigned: {task.assignedToName}</span>
              )}
            </div>

            {/* Actions — always visible on mobile, hover on desktop */}
            <div className="flex items-center gap-1.5 sm:gap-2 sm:opacity-0 sm:group-hover:opacity-100 transition-opacity shrink-0">
              <Link
                to={`/tasks/${task.id}`}
                className="text-xs text-blue-600 hover:text-blue-800 font-medium px-2 sm:px-3 py-1 sm:py-1.5
                           border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors"
              >
                Edit
              </Link>
              <button
                onClick={() => onDelete(task.id)}
                disabled={isDeleting}
                className="text-xs text-red-600 hover:text-red-800 font-medium px-2 sm:px-3 py-1 sm:py-1.5
                           border border-red-200 rounded-lg hover:bg-red-50 transition-colors
                           disabled:opacity-50"
              >
                {isDeleting ? '...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return 'morning';
  if (hour < 18) return 'afternoon';
  return 'evening';
}
