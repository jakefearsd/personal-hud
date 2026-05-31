import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { InvestmentsView } from './InvestmentsView';
import '@testing-library/jest-dom';

// Mock the nested components that we aren't testing here
vi.mock('./ComparisonDashboard', () => ({
  ComparisonDashboard: () => <div data-testid="comparison-dashboard" />
}));
vi.mock('./MarketPredictionDashboard', () => ({
  MarketPredictionDashboard: () => <div data-testid="market-prediction-dashboard" />
}));
vi.mock('../api', () => ({
  apiFetch: vi.fn()
}));

const mockPods = [
  {
    id: 'economic_health',
    title: 'Economic Health',
    sentimentNarrative: 'The economy is doing something.',
    metrics: [
      { ticker: 'CPI', label: 'Core Inflation', currentValue: 3.2, historicalPercentile: 85.0, changePercent: -1.5 }
    ]
  }
];

describe('InvestmentsView', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('renders loading state initially', () => {
    globalThis.fetch = vi.fn().mockImplementation(() => new Promise(() => {})); // Never resolves
    render(<InvestmentsView />);
    expect(screen.getByText('Initializing Macro Pods...')).toBeInTheDocument();
  });

  it('fetches and renders macro pods successfully', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      json: () => Promise.resolve(mockPods)
    });

    render(<InvestmentsView />);

    await waitFor(() => {
      expect(screen.getByText('Economic Health Data')).toBeInTheDocument();
      expect(screen.getByText('"The economy is doing something."')).toBeInTheDocument();
      expect(screen.getByText('Core Inflation')).toBeInTheDocument();
    });
  });

  it('handles fetch errors gracefully by removing loading state', async () => {
    globalThis.fetch = vi.fn().mockRejectedValue(new Error('Network error'));
    
    render(<InvestmentsView />);

    await waitFor(() => {
      // It should stop showing the initializing message
      expect(screen.queryByText('Initializing Macro Pods...')).not.toBeInTheDocument();
    });
  });
});
