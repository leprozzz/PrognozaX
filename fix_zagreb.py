import urllib.request
import gzip

url = "https://bulk.meteostat.net/v2/daily/14236.csv.gz"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
res = urllib.request.urlopen(req)

with gzip.GzipFile(fileobj=res) as gz:
    data = gz.read().decode('utf-8').strip().split('\n')

with open("app/src/main/assets/data/csv/zagreb.csv", "w") as f:
    f.write("Date,Tavg,Tmin,Tmax,Prcp\n")
    for line in data:
        if not line: continue
        parts = line.split(',')
        date = parts[0]
        if date >= "1990-01-01":
            tavg = parts[1]
            tmin = parts[2]
            tmax = parts[3]
            prcp = parts[4]
            f.write(f"{date},{tavg},{tmin},{tmax},{prcp}\n")

print("Zagreb (Grič) updated!")
