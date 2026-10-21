import re

with open('app/src/main/java/com/climateinsights/app/data/repository/WeatherRepository.kt', 'r') as f:
    content = f.read()

old_add = '''            mergedList.add(
                DailyRecord(
                    date = date,
                    tavg = finalTavg,
                    tmin = safeTmin,
                    tmax = safeTmax,
                    prcp = finalPrcp
                )
            )'''
            
new_add = '''            mergedList.add(
                DailyRecord(
                    date = date,
                    tavg = sanitizeTemp(finalTavg),
                    tmin = sanitizeTemp(safeTmin),
                    tmax = sanitizeTemp(safeTmax),
                    prcp = finalPrcp
                )
            )'''

if old_add in content:
    content = content.replace(old_add, new_add)
    with open('app/src/main/java/com/climateinsights/app/data/repository/WeatherRepository.kt', 'w') as f:
        f.write(content)
    print("Patched mergedList.add successfully.")
else:
    print("Could not find old_add.")
