import urllib.request
import json
import math

def get_nearby(lat, lon):
    url = f"https://meteostat.net/api/v2/stations/nearby?lat={lat}&lon={lon}&limit=5"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        res = urllib.request.urlopen(req)
        data = json.loads(res.read().decode('utf-8'))
        for s in data['data']:
            print(f"ID: {s['id']}, Name: {s['name']}, Distance: {s['distance']}")
    except Exception as e:
        pass

print("Nearby Bihac:")
get_nearby(44.81, 15.86)
print("Nearby Doboj:")
get_nearby(44.73, 18.08)
