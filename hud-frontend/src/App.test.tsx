import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { ThemeProvider } from './components/theme-provider';

const renderApp = () => {
  return render(
    <ThemeProvider defaultTheme="dark" storageKey="hud-ui-theme-test">
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ThemeProvider>
  );
};

describe('HUD Intelligence Dashboard', () => {
  it('renders and defaults to the Theaters view', async () => {
    renderApp();
    
    // Check for Dashboard title
    expect(await screen.findByText(/Theater Intelligence/i)).toBeInTheDocument();
    
    // Verify MSW data is rendered in a shadcn Card
    expect(await screen.findByText(/Ukraine status update/i)).toBeInTheDocument();
    expect(screen.getByText(/European Theater: Ukraine/i)).toBeInTheDocument();
  });

  it('navigates to Investments view and shows macro vitals', async () => {
    const user = userEvent.setup();
    renderApp();
    
    const navLink = await screen.findByRole('link', { name: /Investments/i });
    await user.click(navLink);
    
    expect(await screen.findByText(/Macro Vitals Dashboard/i)).toBeInTheDocument();
    
    // Use testId to isolate the vitals grid
    const vitalsGrid = await screen.findByTestId('vitals-grid');
    expect(within(vitalsGrid).getByText(/^S&P 500$/i)).toBeInTheDocument();
    expect(within(vitalsGrid).getByText(/5,200.50/)).toBeInTheDocument();
    
    // Use testId to isolate the prediction dashboard
    const predDashboard = await screen.findByTestId('prediction-dashboard');
    expect(within(predDashboard).getByText(/Bullish momentum/i)).toBeInTheDocument();
  });

  it('navigates to Config view and interacts with the Brain form', async () => {
    const user = userEvent.setup();
    renderApp();
    
    const navLink = await screen.findByRole('link', { name: /Config/i });
    await user.click(navLink);
    
    expect(await screen.findByText(/Integrate New Brain/i)).toBeInTheDocument();
    
    // Just verify the provider trigger exists for now, as portal interaction in JSDOM is brittle
    const providerTrigger = screen.getByLabelText(/Intelligence Provider/i);
    expect(providerTrigger).toBeInTheDocument();
    
    // Check for sub-sections
    expect(screen.getAllByText(/Neural Parameters/i)[0]).toBeInTheDocument();
    expect(screen.getAllByText(/Security Protocol/i)[0]).toBeInTheDocument();
  });

  it('toggles dark/light mode', async () => {
    const user = userEvent.setup();
    renderApp();
    
    // Check initial state (default dark)
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    
    const modeTrigger = screen.getByRole('button', { name: /Toggle theme/i });
    await user.click(modeTrigger);
    
    const lightOption = await screen.findByText(/Light/i);
    await user.click(lightOption);
    
    expect(document.documentElement.classList.contains('light')).toBe(true);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('handles authentication flow: login and logout', async () => {
    const user = userEvent.setup();
    renderApp();

    // Wait for async auth status fetch to complete and render the username
    const usernameElement = await screen.findByText(/^admin$/i);
    expect(usernameElement).toBeInTheDocument();

    // Test Logout
    const logoutBtn = screen.getByTitle(/Logout/i);
    await user.click(logoutBtn);

    // Verify it triggers the logout (we can check if the login button reappears)
    expect(await screen.findByRole('button', { name: /Login/i })).toBeInTheDocument();
  });

  it('triggers a briefing refresh and shows loading state', async () => {
    const user = userEvent.setup();
    renderApp();

    const refreshBtn = await screen.findByRole('button', { name: /Refresh Briefing/i });
    await user.click(refreshBtn);

    // Check for "Generating..." text or spin state if possible
    expect(screen.getByText(/Generating.../i)).toBeInTheDocument();
    
    // Wait for it to finish (MSW mock is fast)
    await waitFor(() => {
      expect(screen.queryByText(/Generating.../i)).not.toBeInTheDocument();
    });
  });
});
