import time
from playwright.sync_api import sync_playwright

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    page = browser.new_page()

    # Enable console logging for debugging
    page.on("console", lambda msg: print(f"Browser Console ({msg.type}): {msg.text}"))

    # Try loading the homepage
    print("Navigating to homepage...")
    page.goto('http://localhost:5173')
    page.wait_for_load_state('networkidle')

    # Take a screenshot of the login page
    page.screenshot(path='/tmp/login.png', full_page=True)
    print("Login page screenshot saved to /tmp/login.png")

    # Perform login
    print("Logging in...")
    page.fill('input[type="text"]', 'admin')
    page.fill('input[type="password"]', 'admin')
    page.click('button[type="submit"]')
    page.wait_for_load_state('networkidle')
    
    # Take a screenshot after login
    page.screenshot(path='/tmp/post_login.png', full_page=True)
    print("Post-login screenshot saved to /tmp/post_login.png")

    # Navigate to Investments
    print("Clicking on Investments...")
    try:
        # Looking for the investments link or button
        page.click("text='Investments'")
        page.wait_for_load_state('networkidle')
        time.sleep(2) # Give it time to fetch
        page.screenshot(path='/tmp/investments.png', full_page=True)
        print("Investments page screenshot saved to /tmp/investments.png")
    except Exception as e:
        print(f"Error navigating to investments: {e}")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)
