import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import App from './App';

test('renders strategic briefings on default tab', async () => {
  const mockBriefings = [
    { id: 1, briefingDate: '2026-05-01', category: 'TECHNOLOGY', markdownContent: '## AI Update\nGemma4 is running well.' }
  ];

  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(mockBriefings),
  }));

  render(<App />);

  await waitFor(() => {
    expect(screen.getByText('Technology & AI')).toBeInTheDocument();
    expect(screen.getByText(/Gemma4 is running well/i)).toBeInTheDocument();
  });
});

test('renders financial news articles on Live Feed tab', async () => {
  const mockArticles = [
    { title: 'Market Rally Continues', url: 'https://finance.yahoo.com/news/1' },
    { title: 'Tech Stocks Surge', url: 'https://finance.yahoo.com/news/2' }
  ];

  // Mock global fetch using Vitest utility
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(mockArticles),
  }));

  render(<App />);

  // Switch to Live Feed tab
  const liveFeedTab = screen.getByText(/Live Feed/i);
  fireEvent.click(liveFeedTab);

  // Should eventually show the articles
  await waitFor(() => {
    expect(screen.getByText('Market Rally Continues')).toBeInTheDocument();
    expect(screen.getByText('Tech Stocks Surge')).toBeInTheDocument();
  });

  const links = screen.getAllByRole('link');
  expect(links).toHaveLength(2);
  expect(links[0]).toHaveAttribute('href', 'https://finance.yahoo.com/news/1');
});
