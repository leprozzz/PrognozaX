import urllib.request
import json
import gzip

def fetch_meteostat(station_id, start_date, end_date):
    url = f"https://bulk.meteostat.net/v2/daily/{station_id}.csv.gz"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        res = urllib.request.urlopen(req)
        with gzip.GzipFile(fileobj=res) as gz:
            data = gz.read().decode('utf-8')
            lines = data.strip().split('\n')
            count = 0
            for line in lines:
                if not line: continue
                parts = line.split(',')
                if start_date <= parts[0] <= end_date:
                    count += 1
            print(f"Station {station_id}: found {count} records between {start_date} and {end_date}")
    except Exception as e:
        print(f"Error fetching {station_id}: {e}")

fetch_meteostat("14528", "1990-01-01", "2018-07-20") # Bihac
fetch_meteostat("14554", "1990-01-01", "2018-07-20") # Doboj
fetch_meteostat("14240", "1990-01-01", "1999-09-07") # Zagreb
