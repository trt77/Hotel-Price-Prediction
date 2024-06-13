import pandas as pd
import numpy as np
import random
from datetime import datetime, timedelta

# Configuration
start_date = datetime(2015, 1, 1)
end_date = datetime(2023, 12, 31)
number_of_stays = 100000
room_types = ['Single', 'Double', 'Family']
seasonal_variation = {
    'winter': (0.5, 0.8),   # Prices 50%-80% of base price in winter
    'spring': (0.8, 1.2),   # Prices 80%-120% of base price in spring
    'summer': (1.2, 1.5),   # Prices 120%-150% of base price in summer
    'fall': (0.8, 1.2)      # Prices 80%-120% of base price in fall
}
base_price = {
    'Single': (100, 200),
    'Double': {1: (200, 250), 2: (200, 300)},
    'Family': {3: (350, 400), 4: (400, 420)}
}
inflation_rate = 0.03
special_events = {
    '2022-12-25': 2.0,  # Christmas
    '2023-06-15': 1.5,  # Summer Festival
    '2023-09-20': 1.8   # Local Concert
}

# Determine season based on the date
def get_season(date):
    if date.month in [12, 1, 2]:
        return 'winter'
    elif date.month in [3, 4, 5]:
        return 'spring'
    elif date.month in [6, 7, 8]:
        return 'summer'
    elif date.month in [9, 10, 11]:
        return 'fall'

# Generate stay data
stays = []

for _ in range(number_of_stays):
    stay = {}
    stay['begin_of_stay'] = start_date + timedelta(days=random.randint(0, (end_date - start_date).days))
    stay['end_of_stay'] = stay['begin_of_stay'] + timedelta(days=random.randint(1, 14))
    stay['persons'] = random.choice([1, 2, 3, 4])  # Adjusted for different room types
    stay['room_type'] = random.choice(room_types)
    stay_duration = (stay['end_of_stay'] - stay['begin_of_stay']).days

    # Determine base price based on room type and number of occupants
    if stay['room_type'] == 'Single':
        base = random.uniform(*base_price['Single'])
    elif stay['room_type'] == 'Double':
        if stay['persons'] in base_price['Double']:
            base = random.uniform(*base_price['Double'][stay['persons']])
        else:
            continue  # Skip this iteration if no matching key
    elif stay['room_type'] == 'Family':
        if stay['persons'] in base_price['Family']:
            base = random.uniform(*base_price['Family'][stay['persons']])
        else:
            continue  # Skip this iteration if no matching key

    # Adjust for inflation
    year_diff = stay['begin_of_stay'].year - start_date.year
    price_adjustment = (1 + inflation_rate) ** year_diff
    price = base * price_adjustment * stay_duration

    # Adjust for seasonal variation
    season = get_season(stay['begin_of_stay'])
    season_multiplier = random.uniform(*seasonal_variation[season])
    price *= season_multiplier

    # Adjust for special events
    for event_date, multiplier in special_events.items():
        event_date = datetime.strptime(event_date, '%Y-%m-%d')
        if stay['begin_of_stay'] <= event_date <= stay['end_of_stay']:
            price *= multiplier

    # Add random variation
    price *= random.uniform(0.8, 1.2)

    stay['total_price'] = round(price, 2)
    stays.append(stay)

# Convert to DataFrame and save to CSV
df = pd.DataFrame(stays)
df['stay_id'] = df.index + 1
df = df[['stay_id', 'begin_of_stay', 'end_of_stay', 'persons', 'room_type', 'total_price']]
df.to_csv('generated_stay_data.csv', index=False)

print("Test data generated and saved to 'generated_stay_data.csv'")
