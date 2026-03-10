import { TaskStatus } from '../types';

const config: Record<TaskStatus, { label: string; classes: string }> = {
  TODO:        { label: 'To Do',       classes: 'bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300' },
  IN_PROGRESS: { label: 'In Progress', classes: 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300' },
  DONE:        { label: 'Done',        classes: 'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-300' },
};

export function StatusBadge({ status }: { status: TaskStatus }) {
  const { label, classes } = config[status];
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${classes}`}>
      {label}
    </span>
  );
}
