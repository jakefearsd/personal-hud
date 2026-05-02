import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import App from './App';

test('renders strategic briefings on default theaters tab', async () => {
  const mockBriefings = [
    { id: 1, generatedAt: '2026-05-02T12:00:00', category: 'THEATER_UKRAINE', markdownContent: '## Tactical Update\nBakhmut sector stabilization.' }
  ];

  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(mockBriefings),
  }));

  render(<App />);

  await waitFor(() => {
    expect(screen.getByText('European Theater: Ukraine')).toBeInTheDocument();
    expect(screen.getByText(/Bakhmut sector stabilization/i)).toBeInTheDocument();
  });
});

test('renders financial news articles on Live sub-tab of News', async () => {
  const mockArticles = [
    { title: 'Market Rally Continues', url: 'https://finance.yahoo.com/news/1' }
  ];

  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(mockArticles),
  }));

  render(<App />);

  // Switch to News tab
  const newsTab = screen.getByText(/News/i);
  fireEvent.click(newsTab);

  // Switch to Live sub-tab
  const liveTab = screen.getByText(/Live/i);
  fireEvent.click(liveTab);

  // Should eventually show the articles
  await waitFor(() => {
    expect(screen.getByText('Market Rally Continues')).toBeInTheDocument();
  });
});
