import requests
import json
import time
from datetime import datetime

symbols = {
    "^GSPC": "S&P 500",
    "^DJI": "Dow 30",
    "^VIX": "VIX",
    "BZ=F": "Brent Crude Oil"
}

end_date = int(time.time())
start_date = end_date - 30 * 365 * 24 * 3600

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
}

print("-- HUD Historical Data Bootstrap")
print("-- Generated on: " + datetime.now().isoformat())
print("TRUNCATE TABLE metric_history; -- Optional: Clear existing to avoid mix")

for symbol, name in symbols.items():
    url = f"https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?period1={start_date}&period2={end_date}&interval=1wk"
    print(f"-- Fetching {name} ({symbol})")
    try:
        response = requests.get(url, headers=headers)
        data = response.json()
        
        result = data['chart']['result'][0]
        timestamps = result['timestamp']
        closes = result['indicators']['quote'][0]['close']
        
        prev_close = None
        for ts, close in zip(timestamps, closes):
            if close is None: continue
            
            dt = datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
            change_pct = 0.0
            if prev_close is not None and prev_close != 0:
                change_pct = (close - prev_close) / prev_close * 100
            
            print(f"INSERT INTO metric_history (ticker, price, change_percent, timestamp) VALUES ('{symbol}', {close}, {change_pct}, '{dt}');")
            prev_close = close
            
    except Exception as e:
        print(f"-- Failed to fetch {symbol}: {str(e)}")

print("-- Bootstrap Complete")
