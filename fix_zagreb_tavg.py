import csv

rows = []
with open("app/src/main/assets/data/csv/zagreb.csv", "r") as f:
    reader = csv.reader(f)
    header = next(reader)
    for row in reader:
        date, tavg, tmin, tmax, prcp = row
        if not tavg and tmin and tmax:
            tavg = str(round((float(tmin) + float(tmax)) / 2, 1))
        rows.append([date, tavg, tmin, tmax, prcp])

with open("app/src/main/assets/data/csv/zagreb.csv", "w", newline="") as f:
    writer = csv.writer(f)
    writer.writerow(header)
    writer.writerows(rows)
print("Zagreb Tavg fixed!")
