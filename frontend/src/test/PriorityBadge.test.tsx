import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { PriorityBadge } from '../components/PriorityBadge';

describe('PriorityBadge', () => {
  it('renders "Low" for LOW priority', () => {
    render(<PriorityBadge priority="LOW" />);
    expect(screen.getByText('Low')).toBeInTheDocument();
  });

  it('renders "Medium" for MEDIUM priority', () => {
    render(<PriorityBadge priority="MEDIUM" />);
    expect(screen.getByText('Medium')).toBeInTheDocument();
  });

  it('renders "High" for HIGH priority', () => {
    render(<PriorityBadge priority="HIGH" />);
    expect(screen.getByText('High')).toBeInTheDocument();
  });
});
