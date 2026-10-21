import urllib.request
import json
import gzip
import math

url = "https://bulk.meteostat.net/v2/stations/lite.json.gz"
response = urllib.request.urlopen(url)
data = gzip.decompress(response.read()).decode('utf-8')
stations = json.loads(data)

cities = [
    ("doboj", 44.73, 18.08),
    ("bihac", 44.81, 15.86),
    ("beograd", 44.81, 20.46),
    ("zagreb", 45.81, 15.98)
]

def haversine(lat1, lon1, lat2, lon2):
    R = 6371
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat/2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon/2)**2
    return 2 * R * math.atan2(math.sqrt(a), math.sqrt(1-a))

for cid, clat, clon in cities:
    closest = None
    min_d = 999999
    for s in stations:
        slat = s['location']['latitude']
        slon = s['location']['longitude']
        d = haversine(clat, clon, slat, slon)
        if d < min_d:
            min_d = d
            closest = s
    print(f"{cid}: {closest['id']} ({closest['name']['en']}) - {min_d:.1f} km")
