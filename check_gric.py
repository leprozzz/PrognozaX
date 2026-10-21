import urllib.request
import gzip

url = f"https://bulk.meteostat.net/v2/daily/14236.csv.gz"
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
            if "1990-01-01" <= parts[0] <= "1999-09-07":
                count += 1
        print(f"Station 14236 (Grič): found {count} records between 1990-01-01 and 1999-09-07")
except Exception as e:
    print(f"Error fetching 14236: {e}")
