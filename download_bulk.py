import urllib.request
import gzip
import os

cities = [
    ("bihac", "14528"),
    ("zagreb", "14240")
]

os.makedirs("app/src/main/assets/data/csv", exist_ok=True)

for cid, stn in cities:
    url = f"https://bulk.meteostat.net/v2/daily/{stn}.csv.gz"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        res = urllib.request.urlopen(req)
        data = gzip.decompress(res.read()).decode('utf-8')
        
        lines = data.split('\n')
        with open(f"app/src/main/assets/data/csv/{cid}.csv", "w") as f:
            f.write("Date,Tavg,Tmin,Tmax,Prcp\n")
            for line in lines:
                if not line.strip(): continue
                parts = line.split(',')
                if len(parts) >= 5:
                    date = parts[0]
                    if date.startswith('19') or date.startswith('20'):
                        tavg = parts[1]
                        tmin = parts[2]
                        tmax = parts[3]
                        prcp = parts[4]
                        f.write(f"{date},{tavg},{tmin},{tmax},{prcp}\n")
        print(f"Downloaded {cid} with {stn}")
    except Exception as e:
        print(f"Failed {cid}: {e}")
