import { TaskPriority } from '../types';

const config: Record<TaskPriority, { label: string; classes: string; dot: string }> = {
  LOW:    { label: 'Low',    classes: 'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300', dot: 'bg-slate-400' },
  MEDIUM: { label: 'Medium', classes: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-300', dot: 'bg-yellow-500' },
  HIGH:   { label: 'High',   classes: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300', dot: 'bg-red-500' },
};

export function PriorityBadge({ priority }: { priority: TaskPriority }) {
  const { label, classes, dot } = config[priority];
  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium ${classes}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${dot}`} />
      {label}
    </span>
  );
}
