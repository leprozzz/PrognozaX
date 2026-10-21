import urllib.request
import json
import gzip

url = "https://bulk.meteostat.net/v2/stations/lite.json.gz"
response = urllib.request.urlopen(url)
data = gzip.decompress(response.read()).decode('utf-8')
stations = json.loads(data)

cities = [
    ("banja-luka", 44.7722, 17.1910),
    ("sarajevo", 43.8563, 18.4131),
    ("mostar", 43.3438, 17.8078),
    ("tuzla", 44.5384, 18.6671),
    ("zenica", 44.2017, 17.9040),
    ("bijeljina", 44.7587, 19.2144),
    ("prijedor", 44.9799, 16.7140),
    ("trebinje", 42.7120, 18.3436)
]

import math
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
