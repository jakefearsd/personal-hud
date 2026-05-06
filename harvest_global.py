import requests
import json
import time
from datetime import datetime

symbols = {
    # Core US
    "^GSPC": "S&P 500",
    "^DJI": "Dow 30",
    "^VIX": "VIX (Volatility)",
    # European
    "^GDAXI": "DAX (Germany)",
    "^FTSE": "FTSE 100 (UK)",
    "^STOXX50E": "Euro Stoxx 50",
    # Asian
    "^N225": "Nikkei 225 (Japan)",
    "^HSI": "Hang Seng (HK)",
    "000001.SS": "Shanghai Comp (China)",
    # Bonds
    "AGG": "US Aggregate Bond (AGG)",
    "BNDX": "Intl Aggregate Bond (BNDX)",
    "^TNX": "US 10Y Treasury Yield",
    # Commodities
    "BZ=F": "Brent Crude Oil",
    "CL=F": "WTI Crude Oil",
    "GC=F": "Gold",
    # Crypto
    "BTC-USD": "Bitcoin",
    "ETH-USD": "Ethereum"
}

end_date = int(time.time())
start_date = end_date - 30 * 365 * 24 * 3600

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
}

print("-- HUD Global Asset Bootstrap")
print("-- Generated on: " + datetime.now().isoformat())

for symbol, name in symbols.items():
    url = f"https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?period1={start_date}&period2={end_date}&interval=1wk"
    print(f"-- Fetching {name} ({symbol})")
    try:
        response = requests.get(url, headers=headers)
        if response.status_code != 200:
            print(f"-- Failed {symbol}: Status {response.status_code}")
            continue
            
        data = response.json()
        
        if 'chart' not in data or data['chart']['result'] is None:
             print(f"-- Failed {symbol}: No data in result")
             continue

        result = data['chart']['result'][0]
        timestamps = result.get('timestamp', [])
        indicators = result.get('indicators', {}).get('quote', [{}])[0]
        closes = indicators.get('close', [])
        
        count = 0
        for ts, close in zip(timestamps, closes):
            if close is None: continue
            
            dt = datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
            print(f"INSERT INTO metric_history (ticker, price, change_percent, timestamp) VALUES ('{symbol}', {close}, 0.0, '{dt}') ON CONFLICT (ticker, timestamp) DO NOTHING;")
            count += 1
        
        print(f"-- Inserted {count} points for {symbol}")
        time.sleep(1) # Be polite
            
    except Exception as e:
        print(f"-- Failed to fetch {symbol}: {str(e)}")

print("-- Global Asset Bootstrap Complete")
