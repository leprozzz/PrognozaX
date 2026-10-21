import urllib.request
import json
import gzip
from datetime import datetime, timedelta

def fetch_era5(lat, lon, start_date, end_date):
    url = f"https://archive-api.open-meteo.com/v1/archive?latitude={lat}&longitude={lon}&start_date={start_date}&end_date={end_date}&daily=temperature_2m_mean,temperature_2m_min,temperature_2m_max,precipitation_sum&timezone=auto"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        res = urllib.request.urlopen(req)
        data = json.loads(res.read().decode('utf-8'))
        return data['daily']
    except Exception as e:
        print(f"Error fetching ERA5: {e}")
        return None

cities_to_fix = [
    ("bihac", 44.81, 15.86, "1990-01-01"),
    ("doboj", 44.73, 18.08, "1990-01-01"),
    ("zagreb", 45.81, 15.98, "1990-01-01"),
    ("beograd", 44.81, 20.46, "1980-01-01")
]

for cid, lat, lon, start_d in cities_to_fix:
    # load existing meteostat data
    existing = {}
    with open(f"app/src/main/assets/data/csv/{cid}.csv", "r") as f:
        lines = f.readlines()
        for line in lines[1:]:
            parts = line.strip().split(',')
            if len(parts) >= 5 and parts[0] >= start_d:
                existing[parts[0]] = parts[1:]
    
    # Check earliest date in existing
    earliest = sorted(existing.keys())[0] if existing else "2026-08-19"
    # if it's already well covered, just trim the beginning for Beograd
    
    if cid == "beograd":
        # just write from 1980
        with open(f"app/src/main/assets/data/csv/{cid}.csv", "w") as f:
            f.write("Date,Tavg,Tmin,Tmax,Prcp\n")
            for k in sorted(existing.keys()):
                f.write(f"{k},{','.join(existing[k])}\n")
        print("Fixed beograd")
        continue

    if earliest > start_d:
        # fetch era5 from start_d to earliest - 1 day
        end_d = (datetime.strptime(earliest, "%Y-%m-%d") - timedelta(days=1)).strftime("%Y-%m-%d")
        print(f"Fetching ERA5 for {cid} from {start_d} to {end_d}")
        era5 = fetch_era5(lat, lon, start_d, end_d)
        if era5:
            for i, d in enumerate(era5['time']):
                tavg = str(era5['temperature_2m_mean'][i]) if era5['temperature_2m_mean'][i] is not None else ""
                tmin = str(era5['temperature_2m_min'][i]) if era5['temperature_2m_min'][i] is not None else ""
                tmax = str(era5['temperature_2m_max'][i]) if era5['temperature_2m_max'][i] is not None else ""
                prcp = str(era5['precipitation_sum'][i]) if era5['precipitation_sum'][i] is not None else "0.0"
                if tavg == "" and tmin != "" and tmax != "":
                    tavg = str(round((float(tmin) + float(tmax)) / 2, 1))
                existing[d] = [tavg, tmin, tmax, prcp]
    
    # write back
    with open(f"app/src/main/assets/data/csv/{cid}.csv", "w") as f:
        f.write("Date,Tavg,Tmin,Tmax,Prcp\n")
        for k in sorted(existing.keys()):
            f.write(f"{k},{','.join(existing[k])}\n")
    print(f"Fixed {cid}")
