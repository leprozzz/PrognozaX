import urllib.request
import json
import csv
import time
from datetime import datetime, timedelta
import os

cities = {
    "banja-luka": (44.7722, 17.1910),
    "sarajevo": (43.8563, 18.4131),
    "mostar": (43.3438, 17.8078),
    "tuzla": (44.5384, 18.6671),
    "zenica": (44.2017, 17.9040),
    "bijeljina": (44.7587, 19.2144),
    "prijedor": (44.9799, 16.7140),
    "trebinje": (42.7120, 18.3436),
    "doboj": (44.73, 18.08),
    "bihac": (44.81, 15.86),
    "beograd": (44.81, 20.46),
    "zagreb": (45.81, 15.98)
}

def fetch_era5(lat, lon, start_date, end_date):
    url = f"https://archive-api.open-meteo.com/v1/archive?latitude={lat}&longitude={lon}&start_date={start_date}&end_date={end_date}&daily=temperature_2m_mean,temperature_2m_min,temperature_2m_max,precipitation_sum&timezone=auto"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        res = urllib.request.urlopen(req)
        data = json.loads(res.read().decode('utf-8'))
        
        era5_dict = {}
        if 'daily' in data and 'time' in data['daily']:
            for i, d in enumerate(data['daily']['time']):
                era5_dict[d] = {
                    "tavg": str(data['daily']['temperature_2m_mean'][i]) if data['daily']['temperature_2m_mean'][i] is not None else "",
                    "tmin": str(data['daily']['temperature_2m_min'][i]) if data['daily']['temperature_2m_min'][i] is not None else "",
                    "tmax": str(data['daily']['temperature_2m_max'][i]) if data['daily']['temperature_2m_max'][i] is not None else "",
                    "prcp": str(data['daily']['precipitation_sum'][i]) if data['daily']['precipitation_sum'][i] is not None else "0.0"
                }
        return era5_dict
    except Exception as e:
        print(f"Error fetching ERA5: {e}")
        return {}

for cid, (lat, lon) in cities.items():
    print(f"Processing {cid}...")
    csv_path = f"app/src/main/assets/data/csv/{cid}.csv"
    
    rows = []
    with open(csv_path, "r") as f:
        reader = csv.reader(f)
        header = next(reader)
        for row in reader:
            rows.append(row)
    
    if not rows:
        continue
        
    start_date = min(row[0] for row in rows)
    end_date = max(row[0] for row in rows)
    
    # Open-Meteo ERA5 limit is 1940
    if start_date < "1940-01-01":
        start_date = "1940-01-01"
        
    era5_data = fetch_era5(lat, lon, start_date, end_date)
    time.sleep(2.5) # Sleep to avoid rate limits
    
    for row in rows:
        date, tavg, tmin, tmax, prcp = row
        
        if not tavg and tmin and tmax:
            tavg = str(round((float(tmin) + float(tmax)) / 2, 1))
            
        if date in era5_data:
            e = era5_data[date]
            if not tavg: tavg = e['tavg']
            if not tmin: tmin = e['tmin']
            if not tmax: tmax = e['tmax']
            if not prcp: prcp = e['prcp']
            
        row[1] = tavg
        row[2] = tmin
        row[3] = tmax
        row[4] = prcp

    with open(csv_path, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(header)
        writer.writerows(rows)
    print(f"Finished {cid}")
