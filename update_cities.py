import urllib.request, gzip, json, csv, os, time
from datetime import datetime

cities = [
    ('bihac', 44.81, 15.86, '14528'),
    ('doboj', 44.73, 18.08, '14554')
]

for city, lat, lon, station in cities:
    print(f"Processing {city}...")
    
    url = f'https://bulk.meteostat.net/v2/daily/{station}.csv.gz'
    meteostat = {}
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        res = urllib.request.urlopen(req)
        data = gzip.decompress(res.read()).decode('utf-8')
        lines = [l for l in data.split('\n') if l.strip()]
        for l in lines:
            parts = l.strip().split(',')
            if len(parts) >= 5:
                d = parts[0]
                meteostat[d] = {
                    'tavg': parts[1] if parts[1] != '' else None,
                    'tmin': parts[2] if parts[2] != '' else None,
                    'tmax': parts[3] if parts[3] != '' else None,
                    'prcp': parts[4] if parts[4] != '' else None
                }
    except Exception as e:
        print(f"Could not fetch meteostat for {city}: {e}")
        
    era5_url = f'https://archive-api.open-meteo.com/v1/archive?latitude={lat}&longitude={lon}&start_date=1990-01-01&end_date=2026-03-18&daily=temperature_2m_mean,temperature_2m_min,temperature_2m_max,precipitation_sum&timezone=auto'
    
    success = False
    for attempt in range(5):
        try:
            req = urllib.request.Request(era5_url, headers={'User-Agent': 'Mozilla/5.0'})
            res = urllib.request.urlopen(req)
            era5_json = json.loads(res.read().decode('utf-8'))['daily']
            success = True
            break
        except Exception as e:
            print(f"Error {e}, waiting 10s...")
            time.sleep(10)
            
    if not success:
        print(f"Failed to fetch ERA5 for {city}")
        continue
    
    era5 = {}
    for i, d in enumerate(era5_json['time']):
        era5[d] = {
            'tavg': str(era5_json['temperature_2m_mean'][i]) if era5_json['temperature_2m_mean'][i] is not None else '',
            'tmin': str(era5_json['temperature_2m_min'][i]) if era5_json['temperature_2m_min'][i] is not None else '',
            'tmax': str(era5_json['temperature_2m_max'][i]) if era5_json['temperature_2m_max'][i] is not None else '',
            'prcp': str(era5_json['precipitation_sum'][i]) if era5_json['precipitation_sum'][i] is not None else '0.0'
        }
        
    combined_rows = []
    all_dates = sorted(era5.keys())
    
    for d in all_dates:
        e = era5[d]
        m = meteostat.get(d, None)
        
        if m is not None:
            tavg = m['tavg'] if m['tavg'] is not None else e['tavg']
            tmin = m['tmin'] if m['tmin'] is not None else e['tmin']
            tmax = m['tmax'] if m['tmax'] is not None else e['tmax']
            prcp = e['prcp'] # FORCE ERA5
        else:
            tavg = e['tavg']
            tmin = e['tmin']
            tmax = e['tmax']
            prcp = e['prcp']
            
        combined_rows.append([d, tavg, tmin, tmax, prcp])
        
    path = f'app/src/main/assets/data/csv/{city}.csv'
    with open(path, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['Date', 'Tavg', 'Tmin', 'Tmax', 'Prcp'])
        writer.writerows(combined_rows)
        
    print(f"Saved {len(combined_rows)} rows for {city}.")
