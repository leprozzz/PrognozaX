import csv
for city in ['banja-luka', 'prijedor', 'bihac', 'doboj']:
    path = f'app/src/main/assets/data/csv/{city}.csv'
    with open(path, 'r') as f:
        reader = list(csv.reader(f))
        print(f'{city}: {len(reader)-1} rows, from {reader[1][0]} to {reader[-1][0]}')
